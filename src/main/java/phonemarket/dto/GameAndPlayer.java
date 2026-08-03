package phonemarket.dto;

import lombok.Data;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;

@Data
public class GameAndPlayer {
    private Game game;
    private GamePlayer player;
    
}
