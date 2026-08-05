package phonemarket.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.ActiveGamesResponse;
import phonemarket.dto.RoomResultResponse;
import phonemarket.service.HomeService;
import phonemarket.service.SessionUserService;

@RestController
@RequestMapping("/api/games")
public class HomeController {

    private final HomeService homeService;
    private final SessionUserService sessionUserService;

    public HomeController(
            HomeService homeService,
            SessionUserService sessionUserService
    ) {
        this.homeService = homeService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping("/create")
    public RoomResultResponse create(HttpSession session) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.create(userId);
    }

    @PostMapping("/{gameId}/join")
    public RoomResultResponse join(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.join(gameId, userId);
    }

    @GetMapping("/{gameId}/room")
    public RoomResultResponse getRoom(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.getRoom(gameId, userId);
    }

    @PostMapping("/{gameId}/leave")
    public RoomResultResponse leave(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.leave(gameId, userId);
    }

    @PostMapping("/{gameId}/abort")
    public RoomResultResponse abort(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.abort(gameId, userId);
    }

    @PostMapping("/{gameId}/start")
    public RoomResultResponse start(
            @PathVariable long gameId,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.start(gameId, userId);
    }

    @GetMapping("/mine/active")
    public ActiveGamesResponse activeGames(HttpSession session) {
        long userId = sessionUserService.requireUserId(session);
        return homeService.getActiveGames(userId);
    }
}
