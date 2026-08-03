package phonemarket.service;

import org.springframework.stereotype.Service;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.PhoneModel;
import phonemarket.entity.RoundAction;
import phonemarket.settlement.ComponentMarketSettlementResult;
import phonemarket.settlement.PlayerCostItem;
import phonemarket.settlement.PlayerCostSettlementResult;
import phonemarket.settlement.SettlementContext;
import phonemarket.settlement.StarSettlementResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PlayerCostSettlementService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(2);

    /*
     * 每名消费者对应的宣传费用。
     */
    private static final BigDecimal FILM_AD_RATE =
            new BigDecimal("100");

    private static final BigDecimal ONLINE_AD_RATE =
            new BigDecimal("25");

    private static final BigDecimal MAGAZINE_AD_RATE =
            new BigDecimal("10");

    /*
     * 单台手机组装费用。
     */
    private static final BigDecimal BASIC_ASSEMBLY_COST =
            new BigDecimal("500");

    private static final BigDecimal MEDIUM_ASSEMBLY_COST =
            new BigDecimal("1000");

    private static final BigDecimal ADVANCED_ASSEMBLY_COST =
            new BigDecimal("2000");

    public PlayerCostSettlementResult calculate(
            SettlementContext context,
            ComponentMarketSettlementResult componentResult,
            StarSettlementResult starResult
    ) {
        validateInput(
                context,
                componentResult,
                starResult
        );

        /*
         * 1. 计算本回合消费者总人数。
         */
        long totalPopulation =
                calculateTotalPopulation(
                        context.getSegmentStates()
                );

        List<PlayerCostItem> items =
                new ArrayList<>();

        Map<Long, PlayerCostItem> itemsByPlayerId =
                new LinkedHashMap<>();

        /*
         * 2. 分别计算每名玩家的成本。
         */
        for (RoundAction action :
                context.getActions()) {

            PhoneModel phoneModel =
                    context.getPhoneModelsById()
                            .get(
                                    action.getPhoneModelId()
                            );

            if (phoneModel == null) {
                throw new IllegalStateException(
                        "玩家成本计算失败：找不到提交对应的手机，actionId="
                                + action.getId()
                );
            }

            PlayerCostItem item =
                    calculatePlayerCost(
                            action,
                            phoneModel,
                            totalPopulation,
                            componentResult,
                            starResult
                    );

            if (itemsByPlayerId.put(
                    item.getGamePlayerId(),
                    item
            ) != null) {
                throw new IllegalStateException(
                        "玩家成本计算失败：玩家成本结果重复，gamePlayerId="
                                + item.getGamePlayerId()
                );
            }

            items.add(item);
        }

        PlayerCostSettlementResult result =
                new PlayerCostSettlementResult();

        result.setTotalPopulation(
                totalPopulation
        );

        result.setItems(items);
        result.setItemsByPlayerId(
                itemsByPlayerId
        );

        return result;
    }

    private PlayerCostItem calculatePlayerCost(
            RoundAction action,
            PhoneModel phoneModel,
            long totalPopulation,
            ComponentMarketSettlementResult componentResult,
            StarSettlementResult starResult
    ) {
        int productionQuantity =
                action.getProductionQuantity();

        /*
         * 3. 查询玩家所选六种零部件的实际价格。
         */
        BigDecimal screenPrice =
                componentResult.getActualUnitPrice(
                        "SCREEN",
                        phoneModel.getScreenLevel()
                );

        BigDecimal processorPrice =
                componentResult.getActualUnitPrice(
                        "PROCESSOR",
                        phoneModel.getProcessorLevel()
                );

        BigDecimal bodyPrice =
                componentResult.getActualUnitPrice(
                        "BODY",
                        phoneModel.getBodyLevel()
                );

        BigDecimal batteryPrice =
                componentResult.getActualUnitPrice(
                        "BATTERY",
                        phoneModel.getBatteryLevel()
                );

        BigDecimal storagePrice =
                componentResult.getActualUnitPrice(
                        "STORAGE",
                        phoneModel.getStorageLevel()
                );

        BigDecimal cameraPrice =
                componentResult.getActualUnitPrice(
                        "CAMERA",
                        phoneModel.getCameraLevel()
                );

        /*
         * 4. 单台手机零部件成本。
         */
        BigDecimal componentUnitCost =
                screenPrice
                        .add(processorPrice)
                        .add(bodyPrice)
                        .add(batteryPrice)
                        .add(storagePrice)
                        .add(cameraPrice)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * 5. 全部手机零部件成本。
         */
        BigDecimal componentCost =
                componentUnitCost
                        .multiply(
                                BigDecimal.valueOf(
                                        productionQuantity
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * 6. 计算单台组装费。
         */
        BigDecimal assemblyUnitCost =
                calculateAssemblyUnitCost(
                        phoneModel
                );

        BigDecimal assemblyCost =
                assemblyUnitCost
                        .multiply(
                                BigDecimal.valueOf(
                                        productionQuantity
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * 7. 总生产成本。
         */
        BigDecimal productionCost =
                componentCost
                        .add(assemblyCost)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * 8. 计算三种普通宣传费用。
         */
        BigDecimal filmAdvertisingCost =
                calculateAdvertisingCost(
                        Boolean.TRUE.equals(
                                action.getFilmAd()
                        ),
                        totalPopulation,
                        FILM_AD_RATE
                );

        BigDecimal onlineAdvertisingCost =
                calculateAdvertisingCost(
                        Boolean.TRUE.equals(
                                action.getOnlineAd()
                        ),
                        totalPopulation,
                        ONLINE_AD_RATE
                );

        BigDecimal magazineAdvertisingCost =
                calculateAdvertisingCost(
                        Boolean.TRUE.equals(
                                action.getMagazineAd()
                        ),
                        totalPopulation,
                        MAGAZINE_AD_RATE
                );

        BigDecimal advertisingCost =
                filmAdvertisingCost
                        .add(onlineAdvertisingCost)
                        .add(magazineAdvertisingCost)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * 9. 明星签约费。
         *
         * 只有明星赢家支付。
         */
        BigDecimal starCost = ZERO;

        if (starResult.isHasWinner() &&
                Objects.equals(
                        starResult.getWinnerGamePlayerId(),
                        action.getGamePlayerId()
                )) {

            starCost =
                    money(
                            starResult.getSigningFee()
                    );
        }

        /*
         * 10. 本回合总投入成本。
         */
        BigDecimal totalCost =
                productionCost
                        .add(advertisingCost)
                        .add(starCost)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        PlayerCostItem item =
                new PlayerCostItem();

        item.setGamePlayerId(
                action.getGamePlayerId()
        );

        item.setPhoneModelId(
                action.getPhoneModelId()
        );

        item.setProductionQuantity(
                productionQuantity
        );

        item.setComponentUnitCost(
                componentUnitCost
        );

        item.setComponentCost(
                componentCost
        );

        item.setAssemblyUnitCost(
                assemblyUnitCost
        );

        item.setAssemblyCost(
                assemblyCost
        );

        item.setProductionCost(
                productionCost
        );

        item.setFilmAdvertisingCost(
                filmAdvertisingCost
        );

        item.setOnlineAdvertisingCost(
                onlineAdvertisingCost
        );

        item.setMagazineAdvertisingCost(
                magazineAdvertisingCost
        );

        item.setAdvertisingCost(
                advertisingCost
        );

        item.setStarCost(
                starCost
        );

        item.setTotalCost(
                totalCost
        );

        return item;
    }

    /**
     * 组装费规则：
     *
     * 至少两个三级部件：2000/台。
     * 否则至少两个二级及以上部件：1000/台。
     * 否则：500/台。
     */
    private BigDecimal calculateAssemblyUnitCost(
            PhoneModel phoneModel
    ) {
        int[] levels = {
                phoneModel.getScreenLevel(),
                phoneModel.getProcessorLevel(),
                phoneModel.getBodyLevel(),
                phoneModel.getBatteryLevel(),
                phoneModel.getStorageLevel(),
                phoneModel.getCameraLevel()
        };

        int levelThreeCount = 0;
        int levelTwoOrAboveCount = 0;

        for (int level : levels) {
            if (level < 1 || level > 3) {
                throw new IllegalStateException(
                        "玩家成本计算失败：手机零部件等级错误，phoneModelId="
                                + phoneModel.getId()
                );
            }

            if (level == 3) {
                levelThreeCount++;
            }

            if (level >= 2) {
                levelTwoOrAboveCount++;
            }
        }

        if (levelThreeCount >= 2) {
            return ADVANCED_ASSEMBLY_COST
                    .setScale(2);
        }

        if (levelTwoOrAboveCount >= 2) {
            return MEDIUM_ASSEMBLY_COST
                    .setScale(2);
        }

        return BASIC_ASSEMBLY_COST
                .setScale(2);
    }

    private BigDecimal calculateAdvertisingCost(
            boolean selected,
            long totalPopulation,
            BigDecimal rate
    ) {
        if (!selected) {
            return ZERO;
        }

        return BigDecimal.valueOf(
                        totalPopulation
                )
                .multiply(rate)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private long calculateTotalPopulation(
            List<SegmentStateDTO> segmentStates
    ) {
        if (segmentStates == null ||
                segmentStates.isEmpty()) {
            throw new IllegalStateException(
                    "玩家成本计算失败：消费者状态不存在"
            );
        }

        long totalPopulation = 0;

        for (SegmentStateDTO segment :
                segmentStates) {

            if (segment.getPopulation() == null ||
                    segment.getPopulation() < 0) {
                throw new IllegalStateException(
                        "玩家成本计算失败：消费者人数错误，segment="
                                + segment.getSegmentCode()
                );
            }

            totalPopulation =
                    Math.addExact(
                            totalPopulation,
                            segment.getPopulation()
                                    .longValue()
                    );
        }

        return totalPopulation;
    }

    private void validateInput(
            SettlementContext context,
            ComponentMarketSettlementResult componentResult,
            StarSettlementResult starResult
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "玩家成本计算失败：SettlementContext不能为空"
            );
        }

        if (context.getRound() == null ||
                !"PROCESSING".equals(
                        context.getRound()
                                .getStatus()
                )) {
            throw new IllegalStateException(
                    "玩家成本计算失败：回合状态不是PROCESSING"
            );
        }

        if (context.getActions() == null ||
                context.getActions().isEmpty()) {
            throw new IllegalStateException(
                    "玩家成本计算失败：玩家提交不存在"
            );
        }

        if (context.getPhoneModelsById() == null ||
                context.getPhoneModelsById().isEmpty()) {
            throw new IllegalStateException(
                    "玩家成本计算失败：玩家手机不存在"
            );
        }

        if (componentResult == null ||
                componentResult.getItems() == null ||
                componentResult.getItems().size() != 18) {
            throw new IllegalStateException(
                    "玩家成本计算失败：零部件市场结果不完整"
            );
        }

        if (starResult == null) {
            throw new IllegalStateException(
                    "玩家成本计算失败：明星结算结果不存在"
            );
        }

        for (RoundAction action :
                context.getActions()) {

            if (action.getProductionQuantity() == null ||
                    action.getProductionQuantity() < 0) {
                throw new IllegalStateException(
                        "玩家成本计算失败：生产数量错误，actionId="
                                + action.getId()
                );
            }
        }
    }

    private BigDecimal money(
            BigDecimal value
    ) {
        if (value == null) {
            return ZERO;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}