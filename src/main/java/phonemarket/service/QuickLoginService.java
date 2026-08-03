package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.dto.QuickLoginResponse;
import phonemarket.dto.QuickLoginUser;
import phonemarket.mapper.QuickLoginMapper;

@Service
@RequiredArgsConstructor
public class QuickLoginService {

    private final QuickLoginMapper quickLoginMapper;

    @Transactional
    public QuickLoginResponse quickLogin(String rawUsername) {
        String username = normalizeUsername(rawUsername);

        Long existingId =
                quickLoginMapper.findIdByUsername(username);

        if (existingId != null) {
            return new QuickLoginResponse(
                    existingId,
                    username
            );
        }

        QuickLoginUser user = new QuickLoginUser();
        user.setUsername(username);

        try {
            quickLoginMapper.insertUser(user);
        } catch (DuplicateKeyException exception) {
            Long concurrentId =
                    quickLoginMapper.findIdByUsername(username);

            if (concurrentId == null) {
                throw exception;
            }

            return new QuickLoginResponse(
                    concurrentId,
                    username
            );
        }

        return new QuickLoginResponse(
                user.getId(),
                username
        );
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        String username = rawUsername.trim();

        if (username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (username.length() > 50) {
            throw new IllegalArgumentException(
                    "用户名不能超过50个字符"
            );
        }

        return username;
    }
}
