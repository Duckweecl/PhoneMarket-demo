package phonemarket.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.RoundActionRequest;
import phonemarket.dto.RoundActionResponse;
import phonemarket.service.PlayService;
import phonemarket.service.SessionUserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class PlayController {

    private final PlayService playService;
    private final SessionUserService sessionUserService;

    @PostMapping("/{gameId}/rounds/current/actions")
    public RoundActionResponse submitAction(
            @PathVariable long gameId,
            @RequestBody RoundActionRequest request,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return playService.submitAction(userId, gameId, request);
    }
}
