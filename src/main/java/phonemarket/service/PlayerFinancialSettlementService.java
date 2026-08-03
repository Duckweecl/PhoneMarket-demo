package phonemarket.service;

import org.springframework.stereotype.Service;
import phonemarket.entity.GamePlayer;
import phonemarket.entity.RoundAction;
import phonemarket.entity.RoundPlayerResult;
import phonemarket.settlement.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PlayerFinancialSettlementService {

    private static final BigDecimal NORMAL_INTEREST_RATE = new BigDecimal("0.05");
    private static final BigDecimal PAYDAY_INTEREST_RATE = new BigDecimal("0.25");

    public FinancialSettlementResult settle(
            SettlementContext context,
            PlayerCostSettlementResult costResult,
            ConsumerPurchaseSettlementResult purchaseResult,
            StarSettlementResult starResult
    ) {
        List<PlayerFinancialItem> items = new ArrayList<>();
        Map<Long, PlayerFinancialItem> byPlayer = new LinkedHashMap<>();

        for (GamePlayer player : context.getPlayers()) {
            long playerId = player.getId();
            RoundAction action = context.getActionsByPlayerId().get(playerId);
            PlayerCostItem cost = costResult.getByPlayerId(playerId);
            PlayerSalesItem sales = purchaseResult.getByPlayerId(playerId);

            BigDecimal beginningCash = money(player.getCash());
            BigDecimal beginningDebt = money(player.getDebt());
            BigDecimal debtLimit = money(player.getDebtLimit());
            BigDecimal beginningAvailableCredit = debtLimit.subtract(beginningDebt).max(BigDecimal.ZERO).setScale(2);

            BigDecimal totalCost = money(cost.getTotalCost());
            BigDecimal shortage = totalCost.subtract(beginningCash).max(BigDecimal.ZERO);
            BigDecimal newNormalLoan = shortage.min(beginningAvailableCredit).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paydayPrincipal = shortage.subtract(newNormalLoan).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal normalLoanPrincipal = beginningDebt.add(newNormalLoan).setScale(2, RoundingMode.HALF_UP);
            BigDecimal normalLoanInterest = normalLoanPrincipal.multiply(NORMAL_INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal paydayInterest = paydayPrincipal.multiply(PAYDAY_INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);

            BigDecimal cashAfterCost = beginningCash
                    .add(newNormalLoan)
                    .add(paydayPrincipal)
                    .subtract(totalCost)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalRepaymentDue = normalLoanPrincipal
                    .add(normalLoanInterest)
                    .add(paydayPrincipal)
                    .add(paydayInterest)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal cashBeforeRepayment = cashAfterCost.add(sales.getTotalRevenue()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualRepayment = cashBeforeRepayment.min(totalRepaymentDue).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingCash = cashBeforeRepayment.subtract(actualRepayment).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingDebt = totalRepaymentDue.subtract(actualRepayment).setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingAvailableCredit = debtLimit.subtract(endingDebt).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            BigDecimal interestTotal = normalLoanInterest.add(paydayInterest).setScale(2, RoundingMode.HALF_UP);
            BigDecimal roundCashResult = sales.getTotalRevenue()
                    .subtract(totalCost)
                    .subtract(interestTotal)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal salesProfit = sales.getConsumerSalesRevenue()
                    .subtract(cost.getProductionCost())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal roundSettlementProfit = sales.getTotalRevenue()
                    .subtract(cost.getProductionCost())
                    .subtract(interestTotal)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal endingTotalSales = money(player.getTotalSales())
                    .add(sales.getConsumerSalesRevenue())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingCumulativeSalesProfit = money(player.getCumulativeSalesProfit())
                    .add(salesProfit)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal endingTotalSettlementProfit = money(player.getTotalSettlementProfit())
                    .add(roundSettlementProfit)
                    .setScale(2, RoundingMode.HALF_UP);

            RoundPlayerResult row = buildRow(
                    context, action, cost, sales, starResult,
                    beginningCash, beginningDebt, beginningAvailableCredit,
                    newNormalLoan, normalLoanPrincipal, normalLoanInterest,
                    paydayPrincipal, paydayInterest, totalRepaymentDue, actualRepayment,
                    endingCash, endingDebt, endingAvailableCredit,
                    roundCashResult, salesProfit, roundSettlementProfit,
                    endingCumulativeSalesProfit, endingTotalSettlementProfit
            );

            PlayerFinancialItem item = new PlayerFinancialItem();
            item.setGamePlayerId(playerId);
            item.setEndingCash(endingCash);
            item.setEndingDebt(endingDebt);
            item.setEndingAvailableCredit(endingAvailableCredit);
            item.setEndingTotalSales(endingTotalSales);
            item.setEndingCumulativeSalesProfit(endingCumulativeSalesProfit);
            item.setEndingTotalSettlementProfit(endingTotalSettlementProfit);
            item.setRoundPlayerResult(row);
            items.add(item);
            byPlayer.put(playerId, item);
        }

        FinancialSettlementResult result = new FinancialSettlementResult();
        result.setItems(items);
        result.setItemsByPlayerId(byPlayer);
        return result;
    }

    private RoundPlayerResult buildRow(
            SettlementContext context,
            RoundAction action,
            PlayerCostItem cost,
            PlayerSalesItem sales,
            StarSettlementResult starResult,
            BigDecimal beginningCash,
            BigDecimal beginningDebt,
            BigDecimal beginningAvailableCredit,
            BigDecimal newNormalLoan,
            BigDecimal normalLoanPrincipal,
            BigDecimal normalLoanInterest,
            BigDecimal paydayPrincipal,
            BigDecimal paydayInterest,
            BigDecimal totalRepaymentDue,
            BigDecimal actualRepayment,
            BigDecimal endingCash,
            BigDecimal endingDebt,
            BigDecimal endingAvailableCredit,
            BigDecimal roundCashResult,
            BigDecimal salesProfit,
            BigDecimal roundSettlementProfit,
            BigDecimal endingCumulativeSalesProfit,
            BigDecimal endingTotalSettlementProfit
    ) {
        RoundPlayerResult row = new RoundPlayerResult();
        row.setRoundId(context.getRound().getId());
        row.setGamePlayerId(action.getGamePlayerId());
        row.setPhoneModelId(action.getPhoneModelId());
        row.setProductionQuantity(action.getProductionQuantity());
        row.setConsumerSalesQuantity(sales.getConsumerSalesQuantity());
        row.setUnsoldQuantity(sales.getUnsoldQuantity());
        row.setSalePrice(sales.getSalePrice());
        row.setComponentUnitCost(cost.getComponentUnitCost());
        row.setComponentCost(cost.getComponentCost());
        row.setAssemblyUnitCost(cost.getAssemblyUnitCost());
        row.setAssemblyCost(cost.getAssemblyCost());
        row.setProductionCost(cost.getProductionCost());
        row.setFilmAd(Boolean.TRUE.equals(action.getFilmAd()));
        row.setOnlineAd(Boolean.TRUE.equals(action.getOnlineAd()));
        row.setMagazineAd(Boolean.TRUE.equals(action.getMagazineAd()));
        row.setFilmAdvertisingCost(cost.getFilmAdvertisingCost());
        row.setOnlineAdvertisingCost(cost.getOnlineAdvertisingCost());
        row.setMagazineAdvertisingCost(cost.getMagazineAdvertisingCost());
        row.setAdvertisingCost(cost.getAdvertisingCost());
        row.setStarBid(money(action.getStarBid()));
        row.setWonStar(starResult.isHasWinner()
                && Objects.equals(starResult.getWinnerGamePlayerId(), action.getGamePlayerId()));
        row.setStarCost(cost.getStarCost());
        row.setConsumerSalesRevenue(sales.getConsumerSalesRevenue());
        row.setLiquidationUnitPrice(sales.getLiquidationUnitPrice());
        row.setLiquidationRevenue(sales.getLiquidationRevenue());
        row.setTotalRevenue(sales.getTotalRevenue());
        row.setSalesProfit(salesProfit);
        row.setBeginningCash(beginningCash);
        row.setBeginningDebt(beginningDebt);
        row.setBeginningAvailableCredit(beginningAvailableCredit);
        row.setNewNormalLoan(newNormalLoan);
        row.setNormalLoanPrincipal(normalLoanPrincipal);
        row.setNormalLoanInterest(normalLoanInterest);
        row.setPaydayPrincipal(paydayPrincipal);
        row.setPaydayInterest(paydayInterest);
        row.setTotalRepaymentDue(totalRepaymentDue);
        row.setActualRepayment(actualRepayment);
        row.setEndingCash(endingCash);
        row.setEndingDebt(endingDebt);
        row.setEndingAvailableCredit(endingAvailableCredit);
        row.setRoundCashResult(roundCashResult);
        row.setRoundSettlementProfit(roundSettlementProfit);
        row.setEndingCumulativeSalesProfit(endingCumulativeSalesProfit);
        row.setEndingTotalSettlementProfit(endingTotalSettlementProfit);
        return row;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
