package phonemarket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phonemarket.dto.Responce;
import phonemarket.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Responce register(
            @RequestBody Responce request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Responce login(
            @RequestBody Responce request,
            HttpServletRequest servletRequest
    ) {
        Responce response = authService.login(request);

        if (response.isSuccess()) {

            HttpSession session =
                    servletRequest.getSession();

            session.setAttribute(
                    "userId",
                    response.getUserid()
            );
        }

        return response;
    }

    @GetMapping("/me")
    public ResponseEntity<Responce> me(
            HttpServletRequest servletRequest
    ) {
        HttpSession session =
                servletRequest.getSession(false);

        if (session == null) {
            return notLoggedInResponse();
        }

        Long userId =
                (Long) session.getAttribute("userId");

        if (userId == null) {
            return notLoggedInResponse();
        }

        Responce response =
                authService.getCurrentUser(userId);

        if (!response.isSuccess()) {
            session.invalidate();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public Responce logout(
            HttpServletRequest servletRequest
    ) {
        HttpSession session =
                servletRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        Responce response = new Responce();

        response.setSuccess(true);
        response.setMessage("退出登录成功");

        return response;
    }

    private ResponseEntity<Responce> notLoggedInResponse() {

        Responce response = new Responce();

        response.setSuccess(false);
        response.setMessage("用户未登录");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

}