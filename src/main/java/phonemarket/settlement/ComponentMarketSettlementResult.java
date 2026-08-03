package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ComponentMarketSettlementResult {

    private List<ComponentMarketSettlementItem> items;

    /**
     * key格式：SCREEN:1
     */
    private Map<String, ComponentMarketSettlementItem> itemsByKey;

    public ComponentMarketSettlementItem getItem(
            String componentType,
            int componentLevel
    ) {
        return itemsByKey.get(
                buildKey(
                        componentType,
                        componentLevel
                )
        );
    }

    public BigDecimal getActualUnitPrice(
            String componentType,
            int componentLevel
    ) {
        ComponentMarketSettlementItem item =
                getItem(
                        componentType,
                        componentLevel
                );

        if (item == null) {
            throw new IllegalArgumentException(
                    "找不到零部件市场结果："
                            + componentType
                            + ":"
                            + componentLevel
            );
        }

        return item.getActualUnitPrice();
    }

    public static String buildKey(
            String componentType,
            int componentLevel
    ) {
        return componentType
                + ":"
                + componentLevel;
    }
}