package phonemarket.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.GameRoundOverviewResponse;
import phonemarket.dto.GameRoundStatusDTO;
import phonemarket.service.GameRoundOverviewService;
import phonemarket.service.SessionUserService;

@RestController
@RequiredArgsConstructor
public class GameOverviewController {

    private final GameRoundOverviewService gameRoundOverviewService;
    private final SessionUserService sessionUserService;

    @GetMapping("/api/games/{gameId}/overview")
    public GameRoundOverviewResponse getOverview(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return gameRoundOverviewService.getOverview(userId, gameId);
    }

    @GetMapping("/api/games/{gameId}/rounds/status")
    public GameRoundStatusDTO getStatus(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return gameRoundOverviewService.getStatus(userId, gameId);
    }
}
