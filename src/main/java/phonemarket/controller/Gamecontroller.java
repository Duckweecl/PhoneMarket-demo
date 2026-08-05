package phonemarket.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import phonemarket.dto.GameAndPlayer;
import phonemarket.dto.GameRoundOverviewResponse;
import phonemarket.dto.RoomAndPlayers;
import phonemarket.entity.Game;
import phonemarket.service.GameService;

@RestController
public class Gamecontroller {
    private final GameService gameService;
    public Gamecontroller(GameService gameService){
        this.gameService = gameService;
    }

    @PostMapping("{id}/create")
    public RoomAndPlayers createGame(@PathVariable long id){
        return gameService.createGame(id);
    }
    @PostMapping("{id}/find/{gameId}")
    public Game findGame(@PathVariable long id){
        return gameService.findGame(id);
    }
    @PostMapping("{id}/join/{gameId}")
    public RoomAndPlayers join(@PathVariable long id,@PathVariable long gameId){
        return gameService.joinGame(id, gameId);
    }
    @PostMapping("/{userId}/start/{gameId}")
    public GameRoundOverviewResponse startGame(
            @PathVariable long gameId,
            @PathVariable long userId
    ) {
        return gameService.startGame(userId, gameId);
    }
    @PostMapping("{id}/abort/{gameId}")
    public RoomAndPlayers abort(@PathVariable long id,@PathVariable long gameId){
        return gameService.abortGame(id,gameId);
    }
    @PostMapping("{id}/leave/{gameId}")
    public void leave(@PathVariable long id,@PathVariable long gameId){
        gameService.leaveGame(id,gameId);
    }
    @PostMapping("{id}/check/{gameId}")
    public RoomAndPlayers check(@PathVariable long id, @PathVariable long gameId){
        return gameService.getRoomDetail(gameId);
    }


}
