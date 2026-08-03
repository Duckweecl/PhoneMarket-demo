package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CurrentPlayerDTO {
    private Long gamePlayerId;
    private String username;
    private String status;
    private Integer rank;

    private BigDecimal cash;
    private BigDecimal debt;
    private BigDecimal debtLimit;
    private BigDecimal availableCredit;
    private BigDecimal cumulativeSalesProfit;
    private BigDecimal totalSettlementProfit;
}
