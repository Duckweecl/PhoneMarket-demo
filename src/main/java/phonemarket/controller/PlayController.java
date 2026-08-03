package phonemarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import phonemarket.dto.RoundActionRequest;
import phonemarket.dto.RoundActionResponse;
import phonemarket.service.PlayService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class PlayController {

    private final PlayService playService;

    @PostMapping(
            "/{gameId}/rounds/current/actions/{userId}"
    )
    public RoundActionResponse submitAction(
            @PathVariable long gameId,
            @PathVariable long userId,
            @RequestBody RoundActionRequest request
    ) {
        return playService.submitAction(
                userId,
                gameId,
                request
        );
    }
}