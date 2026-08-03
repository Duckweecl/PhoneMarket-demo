package phonemarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.GameRoundOverviewResponse;
import phonemarket.dto.GameRoundStatusDTO;
import phonemarket.service.GameRoundOverviewService;

@RestController
@RequiredArgsConstructor
public class GameOverviewController {

    private final GameRoundOverviewService gameRoundOverviewService;

    @GetMapping("/api/games/{gameId}/overview/{userId}")
    public GameRoundOverviewResponse getOverview(
            @PathVariable long gameId,
            @PathVariable long userId
    ) {
        return gameRoundOverviewService.getOverview(
                userId,
                gameId
        );
    }

    @GetMapping("/api/games/{gameId}/rounds/status/{userId}")
    public GameRoundStatusDTO getStatus(
            @PathVariable long gameId,
            @PathVariable long userId
    ) {
        return gameRoundOverviewService.getStatus(userId, gameId);
    }

}
