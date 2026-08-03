package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GamePlayer {

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
}