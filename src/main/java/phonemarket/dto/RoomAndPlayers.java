package phonemarket.dto;
import lombok.Data;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;

import java.util.List;
@Data
public class RoomAndPlayers {
    private Game game;
    private List<GamePlayer> playerlist;
}

