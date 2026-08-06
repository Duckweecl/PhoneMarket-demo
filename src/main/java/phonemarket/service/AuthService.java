package phonemarket.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.dto.AuthRequest;
import phonemarket.dto.AuthResponse;
import phonemarket.dto.UpdateUsernameRequest;
import phonemarket.entity.User;
import phonemarket.mapper.AuthMapper;

import java.util.Objects;

@Service
public class AuthService {

    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_]+$";
    private static final String NICKNAME_PATTERN =
            "^[A-Za-z\\u4e00-\\u9fa5]+$";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final GameQueryService gameQueryService;

    public AuthService(
            AuthMapper authMapper,
            PasswordEncoder passwordEncoder,
            GameQueryService gameQueryService
    ) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.gameQueryService = gameQueryService;
    }

    public AuthResponse login(AuthRequest request) {
        AuthResponse response = new AuthResponse();

        String username = normalize(request.getUsername());
        String password = request.getPassword();

        if (isBlank(username) || isBlank(password)) {
            return failure(response, "登录失败，用户名或密码错误");
        }

        User user = authMapper.findByUsername(username);
        if (user == null
                || !passwordEncoder.matches(
                        password,
                        user.getPasswordHash()
                )) {
            return failure(response, "登录失败，用户名或密码错误");
        }

        return userResponse(response, user, "登录成功");
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        AuthResponse response = new AuthResponse();

        String username = normalize(request.getUsername());
        String nickname = normalize(request.getNickname());
        String password = request.getPassword();

        String validationMessage = validateRegistration(
                username,
                nickname,
                password
        );

        if (validationMessage != null) {
            return failure(response, validationMessage);
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(password));

        try {
            if (authMapper.register(user) != 1) {
                return failure(response, "注册失败，请稍后重试");
            }
        } catch (DuplicateKeyException exception) {
            return failure(response, "用户名已存在");
        }

        return userResponse(response, user, "注册成功，请返回登录");
    }

    public AuthResponse getCurrentUser(long userId) {
        AuthResponse response = new AuthResponse();
        User user = authMapper.findById(userId);

        if (user == null) {
            return failure(response, "用户不存在");
        }

        return userResponse(response, user, "用户已登录");
    }

    @Transactional
    public AuthResponse updateUsername(
            long userId,
            UpdateUsernameRequest request
    ) {
        AuthResponse response = new AuthResponse();
        User user = authMapper.findById(userId);

        if (user == null) {
            return failure(response, "用户不存在");
        }

        String username = normalize(request.getUsername());
        String validationMessage = validateUsername(username);

        if (validationMessage != null) {
            return failure(response, validationMessage);
        }

        if (Objects.equals(user.getUsername(), username)) {
            return userResponse(response, user, "用户名没有变化");
        }

        try {
            if (authMapper.updateUsername(userId, username) != 1) {
                return failure(response, "修改用户名失败");
            }
        } catch (DuplicateKeyException exception) {
            return failure(response, "用户名已存在");
        }

        user.setUsername(username);

        /*
         * 房间缓存中包含玩家用户名，因此用户资料变化后清空房间缓存。
         */
        gameQueryService.evictAllRoomDetails();

        return userResponse(response, user, "用户名修改成功");
    }

    private String validateRegistration(
            String username,
            String nickname,
            String password
    ) {
        if (isBlank(nickname)) {
            return "昵称不能为空";
        }
        if (nickname.length() < 2) {
            return "昵称不能少于2个字符";
        }
        if (nickname.length() > 20) {
            return "昵称不能超过20个字符";
        }
        if (!nickname.matches(NICKNAME_PATTERN)) {
            return "昵称只能包含中文或英文";
        }

        String usernameMessage = validateUsername(username);
        if (usernameMessage != null) {
            return usernameMessage;
        }

        if (isBlank(password)) {
            return "密码不能为空";
        }
        if (password.length() < 6) {
            return "密码不能少于6个字符";
        }
        if (password.length() > 50) {
            return "密码不能超过50个字符";
        }
        if (Objects.equals(username, password)) {
            return "密码不能与用户名相同";
        }

        return null;
    }

    private String validateUsername(String username) {
        if (isBlank(username)) {
            return "用户名不能为空";
        }
        if (username.length() < 4) {
            return "用户名不能少于4个字符";
        }
        if (username.length() > 20) {
            return "用户名不能超过20个字符";
        }
        if (!username.matches(USERNAME_PATTERN)) {
            return "用户名只能包含字母、数字和下划线";
        }
        return null;
    }

    private AuthResponse userResponse(
            AuthResponse response,
            User user,
            String message
    ) {
        response.setSuccess(true);
        response.setMessage(message);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        return response;
    }

    private AuthResponse failure(
            AuthResponse response,
            String message
    ) {
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
