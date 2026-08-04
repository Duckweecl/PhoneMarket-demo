package phonemarket.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            @RequestBody Responce request
    ) {

        return authService.login(request);
    }
}
