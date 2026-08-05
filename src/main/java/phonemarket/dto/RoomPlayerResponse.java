package phonemarket.dto;

import lombok.Data;

@Data
public class RoomPlayerResponse {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 是否为房主
     */
    private boolean owner;
}