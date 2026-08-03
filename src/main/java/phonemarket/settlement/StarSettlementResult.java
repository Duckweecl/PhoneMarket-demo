package phonemarket.settlement;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StarSettlementResult {

    private Long winnerGamePlayerId;

    private BigDecimal signingFee;

    private String targetSegmentCode;

    private BigDecimal boost;

    private boolean hasWinner;
}