package phonemarket.dto;

import lombok.Data;

/**
 * 登录和注册共用的请求对象。
 * 登录只使用 username、password；注册还会使用 nickname。
 */
@Data
public class AuthRequest {
    private String username;
    private String nickname;
    private String password;
}
