package phonemarket.dto;

import lombok.Data;

@Data
public class GameRoundStatusDTO {
    private Long gameId;
    private String gameStatus;
    private Long currentRoundId;
    private Integer currentRoundNo;
    private String roundStatus;
    private Integer submittedCount;
    private Integer expectedPlayerCount;
    private Boolean currentPlayerSubmitted;
}
