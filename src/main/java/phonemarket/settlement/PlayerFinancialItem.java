package phonemarket.settlement;

import lombok.Data;
import phonemarket.entity.RoundPlayerResult;

import java.math.BigDecimal;

@Data
public class PlayerFinancialItem {
    private Long gamePlayerId;
    private BigDecimal endingCash;
    private BigDecimal endingDebt;
    private BigDecimal endingAvailableCredit;
    private BigDecimal endingTotalSales;
    private BigDecimal endingCumulativeSalesProfit;
    private BigDecimal endingTotalSettlementProfit;
    private RoundPlayerResult roundPlayerResult;
}
