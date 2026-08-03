package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerFinancialResultDTO {
    private Long roundId;
    private Integer roundNo;
    private Long gamePlayerId;

    private BigDecimal beginningCash;
    private BigDecimal beginningDebt;
    private BigDecimal beginningAvailableCredit;

    private Integer productionQuantity;
    private BigDecimal salePrice;
    private BigDecimal componentUnitCost;
    private BigDecimal componentCost;
    private BigDecimal assemblyUnitCost;
    private BigDecimal assemblyCost;
    private BigDecimal productionCost;
    private BigDecimal filmAdvertisingCost;
    private BigDecimal onlineAdvertisingCost;
    private BigDecimal magazineAdvertisingCost;
    private BigDecimal advertisingCost;
    private BigDecimal starBid;
    private Boolean wonStar;
    private BigDecimal starCost;
    private BigDecimal totalOperatingCost;

    private Integer consumerSalesQuantity;
    private Integer unsoldQuantity;
    private BigDecimal consumerSalesRevenue;
    private BigDecimal liquidationUnitPrice;
    private BigDecimal liquidationRevenue;
    private BigDecimal totalRevenue;

    private BigDecimal newNormalLoan;
    private BigDecimal normalLoanPrincipal;
    private BigDecimal normalLoanInterest;
    private BigDecimal paydayPrincipal;
    private BigDecimal paydayInterest;
    private BigDecimal totalRepaymentDue;
    private BigDecimal actualRepayment;

    private BigDecimal endingCash;
    private BigDecimal endingDebt;
    private BigDecimal endingAvailableCredit;
    private BigDecimal roundCashResult;
    private BigDecimal salesProfit;
    private BigDecimal roundSettlementProfit;
    private BigDecimal endingCumulativeSalesProfit;
    private BigDecimal endingTotalSettlementProfit;
}
