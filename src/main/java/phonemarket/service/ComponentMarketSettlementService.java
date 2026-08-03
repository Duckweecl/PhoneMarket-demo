package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.entity.PhoneModel;
import phonemarket.entity.RoundAction;
import phonemarket.mapper.RoundComponentMarketMapper;
import phonemarket.settlement.ComponentMarketSettlementItem;
import phonemarket.settlement.ComponentMarketSettlementResult;
import phonemarket.settlement.ComponentMarketSettlementRow;
import phonemarket.settlement.SettlementContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComponentMarketSettlementService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    /**
     * 单回合最大涨幅：50%。
     */
    private static final BigDecimal MAX_INCREASE =
            new BigDecimal("0.5000");

    /**
     * 单回合最大降幅：50%。
     */
    private static final BigDecimal MAX_DECREASE =
            new BigDecimal("-0.5000");

    /**
     * 需求每少2%，价格降低1%。
     */
    private static final BigDecimal DECREASE_SPEED =
            new BigDecimal("0.5000");

    private final RoundComponentMarketMapper
            componentMarketMapper;

    @Transactional
    public ComponentMarketSettlementResult settle(
            SettlementContext context
    ) {
        validateContext(context);

        long roundId =
                context.getRound().getId();

        /*
         * 1. 根据玩家提交和手机配置，
         * 汇总18种零部件需求。
         */
        Map<String, Integer> demandByKey =
                calculateDemand(context);

        /*
         * 2. 锁定当前回合18条市场记录。
         */
        List<ComponentMarketSettlementRow> rows =
                componentMarketMapper
                        .findSettlementRowsForUpdate(
                                roundId
                        );

        validateRows(
                roundId,
                rows
        );

        /*
         * 3. 判断当前回合是否已经结算。
         */
        long settledCount =
                rows.stream()
                        .filter(row ->
                                row.getPremiumChange()
                                        != null
                        )
                        .count();

        if (settledCount == rows.size()) {
            /*
             * 18条全部已经结算：
             * 直接读取已有结果，不重复涨价。
             */
            return buildExistingResult(
                    rows
            );
        }

        if (settledCount > 0) {
            /*
             * 正常事务中不会出现部分结算。
             * 出现时说明数据状态异常。
             */
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "存在部分已结算、部分未结算的数据"
            );
        }

        /*
         * 4. 逐项计算并保存市场价格。
         */
        List<ComponentMarketSettlementItem> items =
                new ArrayList<>();

        Map<String, ComponentMarketSettlementItem>
                itemsByKey =
                new LinkedHashMap<>();

        for (ComponentMarketSettlementRow row :
                rows) {

            String key =
                    buildKey(
                            row.getComponentType(),
                            row.getComponentLevel()
                    );

            int demandQuantity =
                    demandByKey.getOrDefault(
                            key,
                            0
                    );

            int supplyQuantity =
                    row.getSupplyQuantity();

            BigDecimal oldPremiumFactor =
                    row.getPremiumFactor();

            /*
             * 根据供需计算理论变化值。
             */
            BigDecimal calculatedChange =
                    calculatePremiumChange(
                            demandQuantity,
                            supplyQuantity
                    );

            /*
             * 溢价系数最低为1。
             */
            BigDecimal newPremiumFactor =
                    oldPremiumFactor
                            .add(calculatedChange)
                            .max(ONE)
                            .setScale(
                                    4,
                                    RoundingMode.HALF_UP
                            );

            /*
             * 保存实际应用的变化值。
             *
             * 例如旧系数1.1，理论降幅-0.5：
             * 最终只能降到1，
             * 实际变化值应记录为-0.1。
             */
            BigDecimal appliedChange =
                    newPremiumFactor
                            .subtract(
                                    oldPremiumFactor
                            )
                            .setScale(
                                    4,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal actualUnitPrice =
                    row.getBaseUnitPrice()
                            .multiply(
                                    newPremiumFactor
                            )
                            .setScale(
                                    0,
                                    RoundingMode.HALF_UP
                            );

            int updatedRows =
                    componentMarketMapper.updateSettlementResult(
                                    roundId,
                                    row.getComponentType(),
                                    row.getComponentLevel(),
                                    demandQuantity,
                                    appliedChange,
                                    newPremiumFactor,
                                    actualUnitPrice
                            );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "更新记录失败，component="
                                + key
                );
            }

            ComponentMarketSettlementItem item =
                    buildItem(
                            row,
                            demandQuantity,
                            appliedChange,
                            newPremiumFactor,
                            actualUnitPrice
                    );

            items.add(item);
            itemsByKey.put(key, item);
        }

        ComponentMarketSettlementResult result =
                new ComponentMarketSettlementResult();

        result.setItems(items);
        result.setItemsByKey(itemsByKey);

        return result;
    }

    /**
     * 统计18种零部件的需求。
     */
    private Map<String, Integer> calculateDemand(
            SettlementContext context
    ) {
        Map<String, Integer> demandByKey =
                new HashMap<>();

        for (RoundAction action :
                context.getActions()) {

            Integer productionQuantity =
                    action.getProductionQuantity();

            if (productionQuantity == null ||
                    productionQuantity < 0) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "生产数量错误，actionId="
                                + action.getId()
                );
            }

            PhoneModel phoneModel =
                    context.getPhoneModelsById()
                            .get(
                                    action.getPhoneModelId()
                            );

            if (phoneModel == null) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "提交关联的手机不存在，actionId="
                                + action.getId()
                );
            }

            /*
             * 每生产一台手机，
             * 六种零部件各需要一个。
             */
            addDemand(
                    demandByKey,
                    "SCREEN",
                    phoneModel.getScreenLevel(),
                    productionQuantity
            );

            addDemand(
                    demandByKey,
                    "PROCESSOR",
                    phoneModel.getProcessorLevel(),
                    productionQuantity
            );

            addDemand(
                    demandByKey,
                    "BODY",
                    phoneModel.getBodyLevel(),
                    productionQuantity
            );

            addDemand(
                    demandByKey,
                    "BATTERY",
                    phoneModel.getBatteryLevel(),
                    productionQuantity
            );

            addDemand(
                    demandByKey,
                    "STORAGE",
                    phoneModel.getStorageLevel(),
                    productionQuantity
            );

            addDemand(
                    demandByKey,
                    "CAMERA",
                    phoneModel.getCameraLevel(),
                    productionQuantity
            );
        }

        return demandByKey;
    }

    private void addDemand(
            Map<String, Integer> demandByKey,
            String componentType,
            Integer componentLevel,
            int productionQuantity
    ) {
        if (componentLevel == null ||
                componentLevel < 1 ||
                componentLevel > 3) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "零部件等级错误，componentType="
                            + componentType
                            + "，level="
                            + componentLevel
            );
        }

        String key =
                buildKey(
                        componentType,
                        componentLevel
                );

        demandByKey.merge(
                key,
                productionQuantity,
                Integer::sum
        );
    }

    /**
     * 计算理论溢价变化值。
     *
     * 需求超过供给：
     * 每多1%，溢价增加1%，最大增加50%。
     *
     * 需求低于供给：
     * 每少2%，溢价降低1%，最大降低50%。
     */
    private BigDecimal calculatePremiumChange(
            int demandQuantity,
            int supplyQuantity
    ) {
        if (demandQuantity < 0) {
            throw new IllegalArgumentException(
                    "需求量不能小于0"
            );
        }

        if (supplyQuantity < 0) {
            throw new IllegalArgumentException(
                    "供货量不能小于0"
            );
        }

        /*
         * 无供货时不能做除法。
         */
        if (supplyQuantity == 0) {
            if (demandQuantity == 0) {
                return ZERO.setScale(4);
            }

            return MAX_INCREASE;
        }

        /*
         * 供需相等，不涨不跌。
         */
        if (demandQuantity == supplyQuantity) {
            return ZERO.setScale(4);
        }

        BigDecimal demand =
                BigDecimal.valueOf(
                        demandQuantity
                );

        BigDecimal supply =
                BigDecimal.valueOf(
                        supplyQuantity
                );

        BigDecimal ratio =
                demand.divide(
                        supply,
                        8,
                        RoundingMode.HALF_UP
                );

        /*
         * 需求大于供给：
         *
         * change = demand / supply - 1
         */
        if (demandQuantity > supplyQuantity) {
            BigDecimal increase =
                    ratio.subtract(ONE);

            return increase
                    .min(MAX_INCREASE)
                    .setScale(
                            4,
                            RoundingMode.HALF_UP
                    );
        }

        /*
         * 需求小于供给：
         *
         * change =
         * (demand / supply - 1) × 0.5
         */
        BigDecimal decrease =
                ratio.subtract(ONE)
                        .multiply(
                                DECREASE_SPEED
                        );

        return decrease
                .max(MAX_DECREASE)
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private void validateContext(
            SettlementContext context
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "零部件市场结算失败："
                            + "SettlementContext不能为空"
            );
        }

        if (context.getRound() == null) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "当前回合不存在"
            );
        }

        if (!"PROCESSING".equals(
                context.getRound().getStatus()
        )) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "回合状态不是PROCESSING"
            );
        }

        if (context.getActions() == null ||
                context.getActions().isEmpty()) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "玩家提交不存在"
            );
        }

        if (context.getPhoneModelsById() == null ||
                context.getPhoneModelsById().isEmpty()) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "玩家手机不存在"
            );
        }
    }

    private void validateRows(
            long roundId,
            List<ComponentMarketSettlementRow> rows
    ) {
        if (rows == null ||
                rows.size() != 18) {
            throw new IllegalStateException(
                    "零部件市场结算失败："
                            + "市场记录不是18条，实际="
                            + (rows == null
                            ? 0
                            : rows.size())
            );
        }

        Map<String, ComponentMarketSettlementRow>
                uniqueRows =
                new HashMap<>();

        for (ComponentMarketSettlementRow row :
                rows) {

            if (row.getRoundId() == null ||
                    row.getRoundId() != roundId) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "市场记录不属于当前回合"
                );
            }

            String key =
                    buildKey(
                            row.getComponentType(),
                            row.getComponentLevel()
                    );

            if (uniqueRows.put(key, row) != null) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "市场记录重复，component="
                                + key
                );
            }

            if (row.getSupplyQuantity() == null ||
                    row.getSupplyQuantity() < 0) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "供货量错误，component="
                                + key
                );
            }

            if (row.getBaseUnitPrice() == null ||
                    row.getBaseUnitPrice()
                            .compareTo(ZERO) <= 0) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "基础价格错误，component="
                                + key
                );
            }

            if (row.getPremiumFactor() == null ||
                    row.getPremiumFactor()
                            .compareTo(ONE) < 0) {
                throw new IllegalStateException(
                        "零部件市场结算失败："
                                + "继承的溢价系数错误，component="
                                + key
                );
            }
        }
    }

    private ComponentMarketSettlementResult
    buildExistingResult(
            List<ComponentMarketSettlementRow> rows
    ) {
        List<ComponentMarketSettlementItem> items =
                new ArrayList<>();

        Map<String, ComponentMarketSettlementItem>
                itemsByKey =
                new LinkedHashMap<>();

        for (ComponentMarketSettlementRow row :
                rows) {

            ComponentMarketSettlementItem item =
                    new ComponentMarketSettlementItem();

            item.setComponentType(
                    row.getComponentType()
            );

            item.setComponentLevel(
                    row.getComponentLevel()
            );

            item.setSupplyQuantity(
                    row.getSupplyQuantity()
            );

            item.setDemandQuantity(
                    row.getDemandQuantity()
            );

            item.setBaseUnitPrice(
                    row.getBaseUnitPrice()
            );

            item.setPremiumChange(
                    row.getPremiumChange()
            );

            item.setNewPremiumFactor(
                    row.getPremiumFactor()
            );

            item.setOldPremiumFactor(
                    row.getPremiumFactor()
                            .subtract(
                                    row.getPremiumChange()
                            )
            );

            item.setActualUnitPrice(
                    row.getActualUnitPrice()
            );

            String key =
                    buildKey(
                            row.getComponentType(),
                            row.getComponentLevel()
                    );

            items.add(item);
            itemsByKey.put(key, item);
        }

        ComponentMarketSettlementResult result =
                new ComponentMarketSettlementResult();

        result.setItems(items);
        result.setItemsByKey(itemsByKey);

        return result;
    }

    private ComponentMarketSettlementItem buildItem(
            ComponentMarketSettlementRow row,
            int demandQuantity,
            BigDecimal premiumChange,
            BigDecimal newPremiumFactor,
            BigDecimal actualUnitPrice
    ) {
        ComponentMarketSettlementItem item =
                new ComponentMarketSettlementItem();

        item.setComponentType(
                row.getComponentType()
        );

        item.setComponentLevel(
                row.getComponentLevel()
        );

        item.setSupplyQuantity(
                row.getSupplyQuantity()
        );

        item.setDemandQuantity(
                demandQuantity
        );

        item.setBaseUnitPrice(
                row.getBaseUnitPrice()
        );

        item.setOldPremiumFactor(
                row.getPremiumFactor()
        );

        item.setPremiumChange(
                premiumChange
        );

        item.setNewPremiumFactor(
                newPremiumFactor
        );

        item.setActualUnitPrice(
                actualUnitPrice
        );

        return item;
    }

    private String buildKey(
            String componentType,
            int componentLevel
    ) {
        return ComponentMarketSettlementResult
                .buildKey(
                        componentType,
                        componentLevel
                );
    }
}