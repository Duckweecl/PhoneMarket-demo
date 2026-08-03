package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoundAction {

    private Long id;

    private Long roundId;
    private Long gamePlayerId;
    private Long phoneModelId;

    private Integer productionQuantity;
    private BigDecimal salePrice;

    private Boolean filmAd;
    private Boolean onlineAd;
    private Boolean magazineAd;

    private BigDecimal starBid;

    private LocalDateTime submittedAt;
}