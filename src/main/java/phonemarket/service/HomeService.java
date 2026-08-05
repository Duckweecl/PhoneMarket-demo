package phonemarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.dto.RoomResultResponse;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;
import phonemarket.mapper.GameMapper;

import java.util.List;

@Service
public class HomeService {

    private final GameMapper gameMapper;
    private final GameService gameService;

    public HomeService(
            GameMapper gameMapper,
            GameService gameService
    ) {
        this.gameMapper = gameMapper;
        this.gameService = gameService;
    }


    @Transactional
    public RoomResultResponse create(long userId) {

        RoomResultResponse response =
                new RoomResultResponse();

        try {
            /*
             * createGame 应该同时完成：
             *
             * 1. 创建 game
             * 2. 创建 game_player
             * 3. 将当前用户设为 1 号座位
             */
            RoomAndPlayers createdRoom =
                    gameService.createGame(userId);

            if (createdRoom == null
                    || createdRoom.getGame() == null) {

                response.setSuccess(false);
                response.setMessage("创建房间失败");
                return response;
            }

            long gameId =
                    createdRoom.getGame().getId();

            /*
             * 再查询一次完整的房间信息。
             */
            RoomAndPlayers roomAndPlayers =
                    gameService.getRoomDetail(gameId);

            if (roomAndPlayers == null) {
                response.setSuccess(false);
                response.setMessage("创建成功，但读取房间信息失败");
                return response;
            }

            response.setSuccess(true);
            response.setMessage("创建成功");
            response.setRoomAndPlayers(roomAndPlayers);

            return response;

        } catch (Exception exception) {
            response.setSuccess(false);
            response.setMessage(
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "创建房间失败"
            );

            return response;
        }
    }


    @Transactional(readOnly = true)
    public RoomResultResponse getRoom(
            long gameId,
            long userId
    ) {
        RoomResultResponse response =
                new RoomResultResponse();

        RoomAndPlayers roomAndPlayers =
                gameService.getRoomDetail(gameId);

        if (roomAndPlayers == null
                || roomAndPlayers.getGame() == null) {

            response.setSuccess(false);
            response.setMessage("未找到房间");
            return response;
        }

        List<GamePlayer> playerList =
                roomAndPlayers.getPlayerlist();

        boolean userIsInRoom =
                playerList != null
                        && playerList.stream().anyMatch(
                        player ->
                                player.getUserId() == userId
                );

        if (!userIsInRoom) {
            response.setSuccess(false);
            response.setMessage("当前用户不在这个房间中");
            return response;
        }

        response.setSuccess(true);
        response.setMessage("已找到房间");
        response.setRoomAndPlayers(roomAndPlayers);

        return response;
    }
}