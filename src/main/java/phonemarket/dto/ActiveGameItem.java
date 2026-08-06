package phonemarket.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主页“我参与的比赛”列表中的一项，也是 Redis 缓存值的一部分。
 */
@Data
public class ActiveGameItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long gameId;
    private String status;
    private Integer currentRound;
    private Integer maxRound;
    private Integer playerCount;
    private Integer seatNo;
    private boolean owner;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
}
