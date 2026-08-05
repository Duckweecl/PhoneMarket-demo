package phonemarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phonemarket.dto.RoomResultResponse;
import phonemarket.service.HomeService;

@RestController
@RequestMapping("/api/games")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }


    @PostMapping("/create")
    public ResponseEntity<RoomResultResponse> create(
            HttpServletRequest request
    ) {
        Long userId = getSessionUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        RoomResultResponse response =
                homeService.create(userId);

        if (!response.isSuccess()) {
            return ResponseEntity
                    .badRequest()
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{gameId}/room")
    public ResponseEntity<RoomResultResponse> getRoom(
            @PathVariable long gameId,
            HttpServletRequest request
    ) {
        Long userId = getSessionUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        RoomResultResponse response =
                homeService.getRoom(gameId, userId);

        if (!response.isSuccess()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }


    private Long getSessionUserId(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object value =
                session.getAttribute("userId");

        if (!(value instanceof Number)) {
            return null;
        }

        return ((Number) value).longValue();
    }


    private ResponseEntity<RoomResultResponse>
    unauthorized() {

        RoomResultResponse response =
                new RoomResultResponse();

        response.setSuccess(false);
        response.setMessage("用户未登录");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
}