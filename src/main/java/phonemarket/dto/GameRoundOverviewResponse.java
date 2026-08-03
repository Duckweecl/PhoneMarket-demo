package phonemarket.dto;

import lombok.Data;

import java.util.List;

@Data
public class GameRoundOverviewResponse {
    private Long gameId;
    private String gameStatus;
    private Integer currentRoundNo;
    private Integer maxRound;
    private Integer submittedCount;
    private Integer expectedPlayerCount;
    private Boolean currentPlayerSubmitted;
    private Boolean gameFinished;

    private CurrentPlayerDTO currentPlayer;
    private List<PlayerOverviewDTO> players;
    private CurrentRoundDTO currentRound;
    private PreviousRoundDTO previousRound;
}
