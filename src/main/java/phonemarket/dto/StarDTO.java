package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StarDTO {
    private String targetSegmentCode;
    private Boolean settled;
    private BigDecimal boost;
    private BigDecimal targetSegmentBoost;
    private Long winnerGamePlayerId;
    private String winnerCompanyName;
    private BigDecimal winningBid;
}
