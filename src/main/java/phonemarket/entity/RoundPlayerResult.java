package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoundPlayerResult {
    private Long id;
    private Long roundId;
    private Long gamePlayerId;
    private Long phoneModelId;

    private Integer productionQuantity;
    private Integer consumerSalesQuantity;
    private Integer unsoldQuantity;
    private BigDecimal salePrice;

    private BigDecimal componentUnitCost;
    private BigDecimal componentCost;
    private BigDecimal assemblyUnitCost;
    private BigDecimal assemblyCost;
    private BigDecimal productionCost;

    private Boolean filmAd;
    private Boolean onlineAd;
    private Boolean magazineAd;
    private BigDecimal filmAdvertisingCost;
    private BigDecimal onlineAdvertisingCost;
    private BigDecimal magazineAdvertisingCost;
    private BigDecimal advertisingCost;

    private BigDecimal starBid;
    private Boolean wonStar;
    private BigDecimal starCost;

    private BigDecimal consumerSalesRevenue;
    private BigDecimal liquidationUnitPrice;
    private BigDecimal liquidationRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal salesProfit;

    private BigDecimal beginningCash;
    private BigDecimal beginningDebt;
    private BigDecimal beginningAvailableCredit;
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
    private BigDecimal roundSettlementProfit;
    private BigDecimal endingCumulativeSalesProfit;
    private BigDecimal endingTotalSettlementProfit;
}
