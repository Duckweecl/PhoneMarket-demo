package phonemarket.dto;

import lombok.Data;

/**
 * 登录、注册、当前用户和修改用户名接口的统一返回对象。
 */
@Data
public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String nickname;
}
