package phonemarket.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.mapper.GameMapper;

/**
 * 运行中的游戏若连续 20 分钟没有任何成功提交，则自动结束。
 * 每次成功提交都会由 PlayService 刷新 last_activity_at。
 */
@Service
public class GameTimeoutService {

    private final GameMapper gameMapper;

    public GameTimeoutService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    @Transactional
    public void closeInactiveGames() {
        gameMapper.expireInactiveRunningGames();
    }
}
