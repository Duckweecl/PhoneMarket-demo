package phonemarket.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoomResponse {

    /**
     * 房间对应的比赛 ID
     */
    private Long gameId;

    /**
     * 房间状态：
     * WAITING
     * RUNNING
     * FINISHED
     * ABORTED
     */
    private String status;

    /**
     * 房主用户 ID
     */
    private Long ownerId;

    /**
     * 房主昵称
     */
    private String ownerNickname;

    /**
     * 当前玩家数量
     */
    private Integer currentPlayers;

    /**
     * 最大玩家数量
     */
    private Integer maxPlayers;

    /**
     * 房间中的所有玩家
     */
    private List<RoomPlayerResponse> players;
}