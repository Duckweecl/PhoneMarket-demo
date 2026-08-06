package phonemarket.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.ActiveGamesResponse;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.dto.RoomResultResponse;

@Service
public class HomeService {

    private final GameService gameService;

    public HomeService(GameService gameService) {
        this.gameService = gameService;
    }

    @Transactional
    public RoomResultResponse create(long userId) {
        gameService.abortOwnedWaitingRooms(userId);

        RoomAndPlayers room = gameService.createGame(userId);
        return roomSuccess(
                "创建房间成功",
                room,
                roomUrl(room.getGame().getId())
        );
    }

    @Transactional
    public RoomResultResponse join(long gameId, long userId) {
        RoomAndPlayers room = gameService.joinGame(userId, gameId);
        return roomSuccess(
                "加入房间成功",
                room,
                roomUrl(gameId)
        );
    }

    @Transactional(readOnly = true)
    public RoomResultResponse getRoom(long gameId, long userId) {
        if (!gameService.hasMembership(userId, gameId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "当前用户不属于这个房间"
            );
        }

        return roomSuccess(
                "获取房间成功",
                gameService.getRoomDetail(gameId),
                null
        );
    }

    @Transactional
    public RoomResultResponse leave(long gameId, long userId) {
        gameService.leaveGame(userId, gameId);

        RoomResultResponse response = new RoomResultResponse();
        response.setSuccess(true);
        response.setMessage("离开房间成功");
        response.setRedirectUrl("/home.html");
        return response;
    }

    @Transactional
    public RoomResultResponse abort(long gameId, long userId) {
        RoomAndPlayers room = gameService.abortGame(userId, gameId);
        return roomSuccess(
                "房间已解散",
                room,
                "/home.html"
        );
    }

    @Transactional
    public RoomResultResponse start(long gameId, long userId) {
        gameService.startGame(userId, gameId);

        return roomSuccess(
                "游戏开始成功",
                gameService.getFreshRoomDetail(gameId),
                gameUrl(gameId)
        );
    }

    @Transactional(readOnly = true)
    public ActiveGamesResponse getActiveGames(long userId) {
        ActiveGamesResponse response = new ActiveGamesResponse();
        response.setSuccess(true);
        response.setMessage("获取参与中的比赛成功");
        response.setGames(gameService.getActiveGames(userId));
        return response;
    }

    private RoomResultResponse roomSuccess(
            String message,
            RoomAndPlayers room,
            String redirectUrl
    ) {
        RoomResultResponse response = new RoomResultResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setRoomAndPlayers(room);
        response.setRedirectUrl(redirectUrl);
        return response;
    }

    private String roomUrl(long gameId) {
        return "/room.html?gameId=" + gameId;
    }

    private String gameUrl(long gameId) {
        return "/game.html?gameId=" + gameId;
    }
}
