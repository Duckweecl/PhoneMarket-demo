package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComponentMarketSettlementRow {

    private Long id;
    private Long roundId;

    private String componentType;
    private Integer componentLevel;

    private BigDecimal baseUnitPrice;

    private Integer supplyQuantity;
    private Integer demandQuantity;

    /**
     * NULL表示尚未结算。
     */
    private BigDecimal premiumChange;

    /**
     * 结算前是继承的旧溢价系数；
     * 结算后是更新后的新溢价系数。
     */
    private BigDecimal premiumFactor;

    private BigDecimal actualUnitPrice;
}