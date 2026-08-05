package phonemarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.GameAndPlayer;
import phonemarket.dto.GameRoundOverviewResponse;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;
import phonemarket.entity.Round;
import phonemarket.mapper.GameMapper;
import phonemarket.mapper.GamePlayerMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class GameService {
    private final GameMapper gameMapper;
    private final UserService userService;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundInitializationService roundInitializationService;
    private final GameRoundOverviewService gameRoundOverviewService;

    public GameService(
            GameMapper gameMapper,
            UserService userService, GamePlayerMapper gamePlayerMapper, RoundInitializationService roundInitializationService, GameRoundOverviewService gameRoundOverviewService) {
        this.gameMapper = gameMapper;
        this.gamePlayerMapper = gamePlayerMapper;
        this.roundInitializationService = roundInitializationService;
        this.userService = userService;
        this.gameRoundOverviewService = gameRoundOverviewService;
    }

    @Transactional
    public RoomAndPlayers createGame(long id){
        userService.findUserId(id);
        Game game = new Game();
        RoomAndPlayers roomAndPlayers = new RoomAndPlayers();
        game.setStatus("WAITING");
        game.setCurrentRound(1);
        game.setMaxRound(10);
        game.setPlayerCount(0);
        gameMapper.insert(game);
        GamePlayer player = new GamePlayer();
        player.setGameId(game.getId());

        player.setUserId(id);
        player.setSeatNo(1);
        gameMapper.join(player);
        gameMapper.increasePlayerCount(player.getGameId());
        List<GamePlayer> list = new ArrayList<>();
        list.add(player);
        roomAndPlayers.setGame(gameMapper.Findbygame(game.getId()));
        roomAndPlayers.setPlayerlist(list);
        return roomAndPlayers;
    }



    public GameAndPlayer joinGame(long userId, long gameId) {
        // 1. 检查用户是否存在
        userService.findUserId(userId);

        // 2. 检查对局是否存在
        Game game = findGame(gameId);

        // 3. 检查对局状态
        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "对局已经开始或已经解散"
            );
        }

        // 4. 检查玩家是否已经在对局中
        int count = gameMapper.countPlayer(userId, gameId);

        if (count > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "玩家已经在该对局中"
            );
        }

        // 5. 查询当前正在使用的座位
        java.util.List<Integer> occupiedSeats =
                gameMapper.findActiveSeatNumbers(gameId);

        // 6. 从1～4中寻找第一个空闲座位
        int availableSeat = 0;

        for (int seat = 1; seat <= 4; seat++) {
            if (!occupiedSeats.contains(seat)) {
                availableSeat = seat;
                break;
            }
        }

        // 7. 没有空闲座位，说明房间已满
        if (availableSeat == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "对局人数已满"
            );
        }

        // 8. 创建局内玩家
        GamePlayer player = new GamePlayer();
        player.setGameId(gameId);
        player.setUserId(userId);
        player.setSeatNo(availableSeat);

        // 9. 插入玩家并增加人数
        gameMapper.join(player);
        gameMapper.increasePlayerCount(gameId);

        // 10. 返回最新结果
        GameAndPlayer result = new GameAndPlayer();
        result.setGame(gameMapper.Findbygame(gameId));
        result.setPlayer(gameMapper.findPlayer(player.getId()));

        return result;
    }

    public Game findGame(long id) {

        Game game = gameMapper.Findbygame(id);
        if (game == null){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "对局不存在");
        }
        return game;
    }




    @Transactional
    public GameRoundOverviewResponse startGame(long userId, long gameId) {
        Game game = findGame(gameId);

        if (!"WAITING".equals(game.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "对局已经开始或解散"
            );
        }

        Long ownerUserId = gameMapper.findOwnerUserId(gameId);

        if (ownerUserId == null || ownerUserId.longValue() != userId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "只有1号座位玩家可以开始游戏"
            );
        }

        List<GamePlayer> activePlayers =
                gamePlayerMapper.findActivePlayers(gameId);
//        目前阶段可以单人开始
//        if (activePlayers.size() < 2 || activePlayers.size() > 4) {
//            throw new ResponseStatusException(
//                    HttpStatus.CONFLICT,
//                    "游戏必须有2至4名玩家"
//            );
//        }

        int updated = gameMapper.startGame(
                gameId,
                activePlayers.size()
        );

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "游戏已经开始"
            );
        }

        roundInitializationService.initializeFirstRound(
                gameId,
                activePlayers.size()
        );

        return gameRoundOverviewService.getOverview(
                userId,
                gameId
        );
    }


    @Transactional
    public Game abortGame(long id,long gameId){
        Game game = findGame(gameId);

        if (!Objects.equals(game.getStatus(), "WAITING")){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "对局已经开始/解散"
            );}
        if (findOwnerUserId(gameId) != id){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "只有1号座位玩家可以解散房间"
            );
        }
        gameMapper.abortGame(gameId);
        gameMapper.playersdismiss(gameId);
        return gameMapper.Findbygame(gameId);


        }
    @Transactional
    public String leaveGame(long userId,long gameId){
        if(gameMapper.playerLeft(userId,gameId) == 1){

            gameMapper.decreasePlayerCount(gameId);
            return "退出成功";
        }
        else{
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "玩家不存在此房间，退出失败");
        }

    }

    public long findOwnerUserId(long gameId) {
        Long userId = gameMapper.findOwnerUserId(gameId);

        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "找不到创建者"
            );
        }

        return userId;
    }

    public RoomAndPlayers getRoomDetail(long gameId){
        RoomAndPlayers roomAndPlayers = new RoomAndPlayers();
        roomAndPlayers.setGame(findGame(gameId));
        roomAndPlayers.setPlayerlist(gameMapper.findPlayersInRoom(gameId));
        return roomAndPlayers;
    }


}
