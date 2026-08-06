package phonemarket.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GamePlayer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long gameId;
    private Long userId;
    private Integer seatNo;

    private BigDecimal cash;
    private BigDecimal debt;
    private BigDecimal debtLimit;
    private BigDecimal totalSales;
    private BigDecimal cumulativeSalesProfit;
    private BigDecimal totalSettlementProfit;

    private String status;
    private LocalDateTime joinedAt;

    /*
     * 以下两个字段来自房间查询时与 user 表的 JOIN，
     * 不属于 game_player 表本身。
     */
    private String username;
    private String nickname;
}
