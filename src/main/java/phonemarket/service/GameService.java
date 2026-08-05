package phonemarket.service;

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

@Service
public class GameService {

    private static final int MAX_PLAYERS = 4;

    private final GameMapper gameMapper;
    private final AuthMapper authMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundInitializationService roundInitializationService;
    private final GameRoundOverviewService gameRoundOverviewService;

    public GameService(
            GameMapper gameMapper,
            AuthMapper authMapper,
            GamePlayerMapper gamePlayerMapper,
            RoundInitializationService roundInitializationService,
            GameRoundOverviewService gameRoundOverviewService
    ) {
        this.gameMapper = gameMapper;
        this.authMapper = authMapper;
        this.gamePlayerMapper = gamePlayerMapper;
        this.roundInitializationService = roundInitializationService;
        this.gameRoundOverviewService = gameRoundOverviewService;
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

        return getRoomDetail(game.getId());
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
            return getRoomDetail(gameId);
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

        if (gameMapper.join(player) != 1
                || gameMapper.increasePlayerCount(gameId) != 1) {
            throw new IllegalStateException("加入房间失败");
        }

        return getRoomDetail(gameId);
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
        return getRoomDetail(gameId);
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
    }

    public RoomAndPlayers getRoomDetail(long gameId) {
        RoomAndPlayers roomAndPlayers = new RoomAndPlayers();
        roomAndPlayers.setGame(findGame(gameId));
        roomAndPlayers.setPlayerlist(
                gameMapper.findPlayersInRoom(gameId)
        );
        return roomAndPlayers;
    }

    public boolean hasMembership(long userId, long gameId) {
        return gameMapper.countMembership(userId, gameId) > 0;
    }

    public List<ActiveGameItem> getActiveGames(long userId) {
        return gameMapper.findActiveGamesForUser(userId);
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
        }
    }
}
