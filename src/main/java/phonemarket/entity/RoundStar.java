package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundStar {

    private Long id;

    private Long roundId;

    private String targetSegmentCode;

    private BigDecimal boost;

    private Long winnerGamePlayerId;

    private BigDecimal winningBid;
}