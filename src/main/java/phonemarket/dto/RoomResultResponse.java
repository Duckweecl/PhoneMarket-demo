package phonemarket.dto;

import lombok.Data;

@Data
public class RoomResultResponse {

    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 返回给前端的提示信息
     */
    private String message;

    /**
     * 房间详细信息
     */
    private RoomAndPlayers roomAndPlayers;
}