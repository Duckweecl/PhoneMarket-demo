package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComponentMarketSettlementItem {

    private String componentType;
    private Integer componentLevel;

    private Integer supplyQuantity;
    private Integer demandQuantity;

    private BigDecimal baseUnitPrice;

    private BigDecimal oldPremiumFactor;
    private BigDecimal premiumChange;
    private BigDecimal newPremiumFactor;

    private BigDecimal actualUnitPrice;
}