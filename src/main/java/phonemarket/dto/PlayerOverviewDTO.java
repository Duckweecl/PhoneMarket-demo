package phonemarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerOverviewDTO {
    private Long gamePlayerId;
    private Long userId;
    private Integer seatNo;
    private String username;
    private String status;
    private Integer rank;

    private BigDecimal cash;
    private BigDecimal debt;
    private BigDecimal debtLimit;
    private BigDecimal availableCredit;
    private BigDecimal cumulativeSalesProfit;
    @JsonIgnore
    private BigDecimal totalSettlementProfit;

    private Boolean currentPlayer;
}
