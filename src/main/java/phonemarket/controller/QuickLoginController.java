package phonemarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.QuickLoginResponse;
import phonemarket.service.QuickLoginService;

@RestController
@RequiredArgsConstructor
public class QuickLoginController {

    private final QuickLoginService quickLoginService;

    @PostMapping("/api/quick-login/{username}")
    public QuickLoginResponse quickLogin(
            @PathVariable String username
    ) {
        return quickLoginService.quickLogin(username);
    }
}
