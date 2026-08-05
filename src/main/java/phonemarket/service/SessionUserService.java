package phonemarket.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 集中处理 Session 中的当前用户，避免每个 Controller 重复编写转换代码。
 */
@Service
public class SessionUserService {

    public long requireUserId(HttpSession session) {
        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户未登录"
            );
        }

        Object value = session.getAttribute("userId");
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户未登录"
            );
        }

        return number.longValue();
    }
}
