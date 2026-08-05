package phonemarket.dto;

import lombok.Data;

/**
 * 创建、加入、查询、开始、离开和解散房间的统一返回对象。
 */
@Data
public class RoomResultResponse {
    private boolean success;
    private String message;
    private RoomAndPlayers roomAndPlayers;

    /**
     * 前端下一步建议进入的页面，例如 /room.html?gameId=1。
     */
    private String redirectUrl;
}
