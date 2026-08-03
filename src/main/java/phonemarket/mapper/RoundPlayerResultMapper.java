package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.RoundPlayerResult;

import java.util.List;

@Mapper
public interface RoundPlayerResultMapper {

    @Insert("""
        <script>
        INSERT INTO round_player_result (
            round_id, game_player_id, phone_model_id,
            production_quantity, consumer_sales_quantity, unsold_quantity, sale_price,
            component_unit_cost, component_cost, assembly_unit_cost, assembly_cost, production_cost,
            film_ad, online_ad, magazine_ad,
            film_advertising_cost, online_advertising_cost, magazine_advertising_cost, advertising_cost,
            star_bid, won_star, star_cost,
            consumer_sales_revenue, liquidation_unit_price, liquidation_revenue, total_revenue, sales_profit,
            beginning_cash, beginning_debt, beginning_available_credit,
            new_normal_loan, normal_loan_principal, normal_loan_interest,
            payday_principal, payday_interest, total_repayment_due, actual_repayment,
            ending_cash, ending_debt, ending_available_credit,
            round_cash_result, round_settlement_profit,
            ending_cumulative_sales_profit, ending_total_settlement_profit
        ) VALUES
        <foreach collection="items" item="item" separator=",">
        (
            #{item.roundId}, #{item.gamePlayerId}, #{item.phoneModelId},
            #{item.productionQuantity}, #{item.consumerSalesQuantity}, #{item.unsoldQuantity}, #{item.salePrice},
            #{item.componentUnitCost}, #{item.componentCost}, #{item.assemblyUnitCost}, #{item.assemblyCost}, #{item.productionCost},
            #{item.filmAd}, #{item.onlineAd}, #{item.magazineAd},
            #{item.filmAdvertisingCost}, #{item.onlineAdvertisingCost}, #{item.magazineAdvertisingCost}, #{item.advertisingCost},
            #{item.starBid}, #{item.wonStar}, #{item.starCost},
            #{item.consumerSalesRevenue}, #{item.liquidationUnitPrice}, #{item.liquidationRevenue}, #{item.totalRevenue}, #{item.salesProfit},
            #{item.beginningCash}, #{item.beginningDebt}, #{item.beginningAvailableCredit},
            #{item.newNormalLoan}, #{item.normalLoanPrincipal}, #{item.normalLoanInterest},
            #{item.paydayPrincipal}, #{item.paydayInterest}, #{item.totalRepaymentDue}, #{item.actualRepayment},
            #{item.endingCash}, #{item.endingDebt}, #{item.endingAvailableCredit},
            #{item.roundCashResult}, #{item.roundSettlementProfit},
            #{item.endingCumulativeSalesProfit}, #{item.endingTotalSettlementProfit}
        )
        </foreach>
        </script>
        """)
    int batchInsert(@Param("items") List<RoundPlayerResult> items);

    @Select("""
        SELECT *
        FROM round_player_result
        WHERE round_id = #{roundId}
        ORDER BY game_player_id
        """)
    List<RoundPlayerResult> findByRoundId(@Param("roundId") long roundId);

    @Select("""
        SELECT *
        FROM round_player_result
        WHERE round_id = #{roundId}
          AND game_player_id = #{gamePlayerId}
        LIMIT 1
        """)
    RoundPlayerResult findByRoundAndPlayer(
            @Param("roundId") long roundId,
            @Param("gamePlayerId") long gamePlayerId
    );

    @Select("SELECT COUNT(*) FROM round_player_result WHERE round_id = #{roundId}")
    int countByRoundId(@Param("roundId") long roundId);
}
