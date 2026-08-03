package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerSalesItem {
    private Long gamePlayerId;
    private Long phoneModelId;
    private Integer productionQuantity;
    private Integer consumerSalesQuantity;
    private Integer unsoldQuantity;
    private BigDecimal salePrice;
    private BigDecimal consumerSalesRevenue;
    private BigDecimal componentUnitCost;
    private BigDecimal liquidationUnitPrice;
    private BigDecimal liquidationRevenue;
    private BigDecimal totalRevenue;
}
