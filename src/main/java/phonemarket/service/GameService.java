package phonemarket.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.ActiveGameItem;
import phonemarket.dto.GameRoundOverviewResponse;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;
import phonemarket.mapper.AuthMapper;
import phonemarket.mapper.GameMapper;
import phonemarket.mapper.GamePlayerMapper;

import java.util.List;

/**
 * 游戏和房间的写业务。
 *
 * 当前项目是单服务器部署，因此不引入分布式锁。
 * 并发安全主要依靠：
 * 1. Spring 事务；
 * 2. UPDATE 中的状态条件；
 * 3. 数据库唯一约束；
 * 4. 房间最多四人的座位检查。
 */
@Service
public class GameService {

    private static final int MAX_PLAYERS = 4;

    private final GameMapper gameMapper;
    private final AuthMapper authMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundInitializationService roundInitializationService;
    private final GameRoundOverviewService gameRoundOverviewService;
    private final GameQueryService gameQueryService;

    public GameService(
            GameMapper gameMapper,
            AuthMapper authMapper,
            GamePlayerMapper gamePlayerMapper,
            RoundInitializationService roundInitializationService,
            GameRoundOverviewService gameRoundOverviewService,
            GameQueryService gameQueryService
    ) {
        this.gameMapper = gameMapper;
        this.authMapper = authMapper;
        this.gamePlayerMapper = gamePlayerMapper;
        this.roundInitializationService = roundInitializationService;
        this.gameRoundOverviewService = gameRoundOverviewService;
        this.gameQueryService = gameQueryService;
    }

    @Transactional
    public RoomAndPlayers createGame(long userId) {
        requireUser(userId);

        Game game = new Game();
        game.setStatus("WAITING");
        game.setCurrentRound(1);
        game.setMaxRound(10);
        game.setPlayerCount(0);

        if (gameMapper.insert(game) != 1) {
            throw new IllegalStateException("创建游戏记录失败");
        }

        GamePlayer owner = new GamePlayer();
        owner.setGameId(game.getId());
        owner.setUserId(userId);
        owner.setSeatNo(1);

        if (gameMapper.join(owner) != 1
                || gameMapper.increasePlayerCount(game.getId()) != 1) {
            throw new IllegalStateException("创建房主记录失败");
        }

        gameQueryService.evictGameState(game.getId());
        return gameQueryService.loadRoomDetailFromDatabase(game.getId());
    }

    @Transactional
    public RoomAndPlayers joinGame(long userId, long gameId) {
        requireUser(userId);
        Game game = findGame(gameId);

        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "房间已经开始或已经解散"
            );
        }

        if (gameMapper.countPlayer(userId, gameId) > 0) {
            return gameQueryService.getRoomDetail(gameId);
        }

        List<Integer> occupiedSeats =
                gameMapper.findActiveSeatNumbers(gameId);

        int availableSeat = findAvailableSeat(occupiedSeats);
        if (availableSeat == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "房间人数已满"
            );
        }

        GamePlayer player = new GamePlayer();
        player.setGameId(gameId);
        player.setUserId(userId);
        player.setSeatNo(availableSeat);

        try {
            if (gameMapper.join(player) != 1
                    || gameMapper.increasePlayerCount(gameId) != 1) {
                throw new IllegalStateException("加入房间失败");
            }
        } catch (DuplicateKeyException exception) {
            /*
             * 单机环境仍可能同时收到两个加入请求。
             * 数据库唯一索引负责最终兜底，事务会自动回滚。
             */
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "房间状态已经变化，请重新尝试加入",
                    exception
            );
        }

        gameQueryService.evictGameState(gameId);
        return gameQueryService.loadRoomDetailFromDatabase(gameId);
    }

    @Transactional
    public GameRoundOverviewResponse startGame(long userId, long gameId) {
        Game game = findGame(gameId);

        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "游戏已经开始或房间已经解散"
            );
        }

        requireOwner(userId, gameId);

        List<GamePlayer> activePlayers =
                gamePlayerMapper.findActivePlayers(gameId);

        if (activePlayers.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "房间内没有可用玩家"
            );
        }

        /*
         * WHERE status = 'WAITING' 是单机并发下的重要保护：
         * 即使用户连续点击，也只有第一次 UPDATE 能成功。
         */
        if (gameMapper.startGame(gameId, activePlayers.size()) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "游戏已经开始"
            );
        }

        roundInitializationService.initializeFirstRound(
                gameId,
                activePlayers.size()
        );

        gameQueryService.evictGameState(gameId);
        return gameRoundOverviewService.getOverview(userId, gameId);
    }

    @Transactional
    public RoomAndPlayers abortGame(long userId, long gameId) {
        Game game = findGame(gameId);

        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "只有等待中的房间可以解散"
            );
        }

        requireOwner(userId, gameId);
        abortWaitingGameWithoutOwnerCheck(gameId);
        return gameQueryService.loadRoomDetailFromDatabase(gameId);
    }

    /**
     * 房主创建新房间前，自动解散自己仍处于 WAITING 的旧房间。
     */
    @Transactional
    public void abortOwnedWaitingRooms(long userId) {
        List<Long> gameIds =
                gameMapper.findWaitingGameIdsOwnedByUser(userId);

        for (Long gameId : gameIds) {
            abortWaitingGameWithoutOwnerCheck(gameId);
        }
    }

    @Transactional
    public void leaveGame(long userId, long gameId) {
        Game game = findGame(gameId);

        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "游戏开始后不能离开房间"
            );
        }

        GamePlayer player =
                gameMapper.findActivePlayer(userId, gameId);

        if (player == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "当前用户不在这个房间中"
            );
        }

        if (player.getSeatNo() != null
                && player.getSeatNo() == 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "房主不能直接离开，请解散房间"
            );
        }

        if (gameMapper.playerLeft(userId, gameId) != 1) {
            throw new IllegalStateException("离开房间失败");
        }

        gameMapper.decreasePlayerCount(gameId);
        gameQueryService.evictGameState(gameId);
    }

    /**
     * 普通页面查询走 Redis 缓存。
     */
    public RoomAndPlayers getRoomDetail(long gameId) {
        return gameQueryService.getRoomDetail(gameId);
    }

    /**
     * 写操作完成后需要立即返回最新数据时直接查数据库。
     */
    public RoomAndPlayers getFreshRoomDetail(long gameId) {
        return gameQueryService.loadRoomDetailFromDatabase(gameId);
    }

    public boolean hasMembership(long userId, long gameId) {
        return gameMapper.countMembership(userId, gameId) > 0;
    }

    public List<ActiveGameItem> getActiveGames(long userId) {
        return gameQueryService.getActiveGames(userId);
    }

    public Game findGame(long gameId) {
        Game game = gameMapper.findById(gameId);
        if (game == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "游戏不存在"
            );
        }
        return game;
    }

    private void requireUser(long userId) {
        if (authMapper.findById(userId) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "用户不存在"
            );
        }
    }

    private void requireOwner(long userId, long gameId) {
        Long ownerUserId = gameMapper.findOwnerUserId(gameId);

        if (ownerUserId == null
                || ownerUserId.longValue() != userId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "只有房主可以执行这个操作"
            );
        }
    }

    private int findAvailableSeat(List<Integer> occupiedSeats) {
        for (int seat = 1; seat <= MAX_PLAYERS; seat++) {
            if (!occupiedSeats.contains(seat)) {
                return seat;
            }
        }
        return 0;
    }

    private void abortWaitingGameWithoutOwnerCheck(long gameId) {
        int updated = gameMapper.abortGame(gameId);
        if (updated == 1) {
            gameMapper.playersdismiss(gameId);
            gameQueryService.evictGameState(gameId);
        }
    }
}
