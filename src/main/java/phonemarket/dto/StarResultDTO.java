package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StarResultDTO {
    private String targetSegmentCode;
    private BigDecimal boost;
    private BigDecimal targetSegmentBoost;
    private Long winnerGamePlayerId;
    private String winnerCompanyName;
    private BigDecimal winningBid;
    private List<StarBidDTO> bids;
}
