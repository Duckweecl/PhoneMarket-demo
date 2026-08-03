package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundComponentMarket {

    private Long id;

    private Long roundId;

    private String componentType;

    private Integer componentLevel;

    private Integer basePrice;
    private Integer supplyQuantity;
    private Integer demandQuantity;

    private BigDecimal premiumFactor;

    private Integer actualUnitPrice;
    private Integer nextSupplyQuantity;
}