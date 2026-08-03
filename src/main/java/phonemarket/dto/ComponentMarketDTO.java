package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ComponentMarketDTO {

    private String componentType;
    private Integer componentLevel;

    private Integer basePrice;
    private Integer supplyQuantity;
    private Integer demandQuantity;

    private BigDecimal premiumFactor;
    private Integer actualUnitPrice;
    private Integer nextSupplyQuantity;
    private Integer previousDemandQuantity;
    private Integer previousSupplyQuantity;
    private Integer supplyChange;
}
