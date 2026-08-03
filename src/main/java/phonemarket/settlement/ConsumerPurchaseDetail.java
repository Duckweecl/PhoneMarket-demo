package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsumerPurchaseDetail {
    private Long sourceCohortId;
    private String segmentCode;
    private Long phoneModelId;
    private Long gamePlayerId;
    private Integer purchaseQuantity;
    private BigDecimal performanceScore;
    private BigDecimal advertisingScore;
    private BigDecimal priceScore;
    private BigDecimal finalScore;
}
