package phonemarket.dto;

import lombok.Data;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间详情缓存对象。
 *
 * 实现 Serializable 是因为 RedisCacheManager 使用 JDK 序列化保存缓存值。
 */
@Data
public class RoomAndPlayers implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Game game;
    private List<GamePlayer> playerlist = new ArrayList<>();
}
