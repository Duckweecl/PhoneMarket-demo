package phonemarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.AuthRequest;
import phonemarket.dto.AuthResponse;
import phonemarket.dto.UpdateUsernameRequest;
import phonemarket.service.AuthService;
import phonemarket.service.SessionUserService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;
    private final SessionUserService sessionUserService;

    public LoginController(
            AuthService authService,
            SessionUserService sessionUserService
    ) {
        this.authService = authService;
        this.sessionUserService = sessionUserService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody AuthRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthResponse response = authService.login(request);

        if (response.isSuccess()) {
            HttpSession oldSession =
                    servletRequest.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session =
                    servletRequest.getSession(true);

            session.setAttribute(
                    "userId",
                    response.getUserId()
            );
        }

        return response;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> currentUser(
            HttpServletRequest servletRequest
    ) {
        HttpSession session =
                servletRequest.getSession(false);

        if (session == null) {
            return unauthorized();
        }

        Object value = session.getAttribute("userId");
        if (!(value instanceof Number number)) {
            return unauthorized();
        }

        AuthResponse response =
                authService.getCurrentUser(number.longValue());

        if (!response.isSuccess()) {
            session.invalidate();
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/username")
    public AuthResponse updateUsername(
            @RequestBody UpdateUsernameRequest request,
            HttpSession session
    ) {
        long userId = sessionUserService.requireUserId(session);
        return authService.updateUsername(userId, request);
    }

    @PostMapping("/logout")
    public AuthResponse logout(
            HttpServletRequest servletRequest
    ) {
        HttpSession session =
                servletRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("退出登录成功");
        return response;
    }

    private ResponseEntity<AuthResponse> unauthorized() {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setMessage("用户未登录");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
}
