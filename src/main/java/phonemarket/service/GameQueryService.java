package phonemarket.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.cache.CacheNames;
import phonemarket.dto.ActiveGameItem;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.entity.Game;
import phonemarket.mapper.GameMapper;

import java.util.List;

/**
 * 只负责“查询 + Redis 缓存”。
 *
 * 写业务仍放在 GameService 中。这样查询和修改职责分开，
 * 也避免同一个类内部调用导致 @Cacheable 不生效。
 */
@Service
public class GameQueryService {

    private final GameMapper gameMapper;

    public GameQueryService(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Cacheable(
            cacheNames = CacheNames.ROOM_DETAILS,
            key = "#gameId",
            unless = "#result == null"
    )
    public RoomAndPlayers getRoomDetail(long gameId) {
        return loadRoomDetailFromDatabase(gameId);
    }

    /**
     * 写操作结束后需要返回最新房间信息时，直接查 MySQL，
     * 不读取尚未失效的缓存。
     */
    public RoomAndPlayers loadRoomDetailFromDatabase(long gameId) {
        Game game = gameMapper.findById(gameId);

        if (game == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "游戏不存在"
            );
        }

        RoomAndPlayers roomAndPlayers = new RoomAndPlayers();
        roomAndPlayers.setGame(game);
        roomAndPlayers.setPlayerlist(
                gameMapper.findPlayersInRoom(gameId)
        );
        return roomAndPlayers;
    }

    @Cacheable(
            cacheNames = CacheNames.ACTIVE_GAMES,
            key = "#userId"
    )
    public List<ActiveGameItem> getActiveGames(long userId) {
        return gameMapper.findActiveGamesForUser(userId);
    }

    /**
     * 房间数据发生变化时：
     * 1. 删除该房间缓存；
     * 2. 清空用户参与比赛列表缓存。
     *
     * 当前项目规模很小，activeGames 使用 allEntries 更直观，
     * 避免维护 gameId 到所有用户 ID 的额外映射。
     */
    @Caching(evict = {
            @CacheEvict(
                    cacheNames = CacheNames.ROOM_DETAILS,
                    key = "#gameId"
            ),
            @CacheEvict(
                    cacheNames = CacheNames.ACTIVE_GAMES,
                    allEntries = true
            )
    })
    public void evictGameState(long gameId) {
        // 缓存注解负责实际删除，方法体无需代码。
    }

    /**
     * 用户名或昵称变化后，房间缓存里的玩家展示信息也需要刷新。
     */
    @CacheEvict(
            cacheNames = CacheNames.ROOM_DETAILS,
            allEntries = true
    )
    public void evictAllRoomDetails() {
        // 缓存注解负责实际删除。
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = CacheNames.ROOM_DETAILS,
                    allEntries = true
            ),
            @CacheEvict(
                    cacheNames = CacheNames.ACTIVE_GAMES,
                    allEntries = true
            )
    })
    public void evictAllGameCaches() {
        // 缓存注解负责实际删除。
    }
}
