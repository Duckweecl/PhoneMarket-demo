package phonemarket.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Game implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String status;
    private Integer currentRound;
    private Integer maxRound;
    private Integer playerCount;
    private LocalDateTime createdAt;

    /** 游戏真正开始的时间。 */
    private LocalDateTime startedAt;

    /** 最近一次成功提交方案的时间；开始游戏时也会初始化。 */
    private LocalDateTime lastActivityAt;

    /** 正常结束、超时结束或解散的时间。 */
    private LocalDateTime finishedAt;

    /** NORMAL、INACTIVITY_TIMEOUT 或 OWNER_ABORTED。 */
    private String finishedReason;
}
