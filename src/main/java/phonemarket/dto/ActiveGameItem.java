package phonemarket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主页“我参与的比赛”列表中的一项。
 */
@Data
public class ActiveGameItem {
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
