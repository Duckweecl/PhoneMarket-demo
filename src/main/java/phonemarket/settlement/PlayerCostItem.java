package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerCostItem {

    private Long gamePlayerId;
    private Long phoneModelId;

    private Integer productionQuantity;

    private BigDecimal componentUnitCost;


    private BigDecimal componentCost;


    private BigDecimal assemblyUnitCost;


    private BigDecimal assemblyCost;


    private BigDecimal productionCost;

    private BigDecimal filmAdvertisingCost;
    private BigDecimal onlineAdvertisingCost;
    private BigDecimal magazineAdvertisingCost;


    private BigDecimal advertisingCost;

    private BigDecimal starCost;


    private BigDecimal totalCost;
}