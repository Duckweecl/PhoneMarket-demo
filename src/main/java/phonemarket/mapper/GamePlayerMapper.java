package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.dto.PlayerOverviewDTO;
import phonemarket.entity.GamePlayer;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface GamePlayerMapper {
    @Select("""
        SELECT * FROM game_player
        WHERE game_id = #{gameId} AND status = 'ACTIVE'
        ORDER BY seat_no
        """)
    List<GamePlayer> findActivePlayersByGameId(@Param("gameId") long gameId);

    @Select("""
        SELECT * FROM game_player
        WHERE game_id = #{gameId} AND status = 'ACTIVE'
        ORDER BY seat_no
        """)
    List<GamePlayer> findActivePlayers(@Param("gameId") long gameId);

    @Select("SELECT username FROM `user` WHERE id = #{userId}")
    String findUsernameByUserId(@Param("userId") long userId);

    @Select("""
        SELECT * FROM game_player
        WHERE game_id = #{gameId} AND user_id = #{userId}
        LIMIT 1
        """)
    GamePlayer findByGameAndUser(@Param("gameId") long gameId, @Param("userId") long userId);

    @Select("""
        SELECT
            gp.id AS game_player_id,
            gp.user_id,
            gp.seat_no,
            u.username,
            gp.status,
            gp.cash,
            gp.debt,
            gp.debt_limit,
            GREATEST(gp.debt_limit - gp.debt, 0) AS available_credit,
            gp.cumulative_sales_profit,
            gp.total_settlement_profit,
            CASE WHEN gp.user_id = #{currentUserId} THEN TRUE ELSE FALSE END AS current_player
        FROM game_player gp
        JOIN `user` u ON u.id = gp.user_id
        WHERE gp.game_id = #{gameId}
        ORDER BY gp.seat_no
        """)
    List<PlayerOverviewDTO> findOverviewPlayers(
            @Param("gameId") long gameId,
            @Param("currentUserId") long currentUserId
    );

    @Update("""
        UPDATE game_player
        SET cash = #{cash},
            debt = #{debt},
            total_sales = #{totalSales},
            cumulative_sales_profit = #{cumulativeSalesProfit},
            total_settlement_profit = #{totalSettlementProfit}
        WHERE id = #{gamePlayerId}
          AND game_id = #{gameId}
        """)
    int updateFinancialState(
            @Param("gamePlayerId") long gamePlayerId,
            @Param("gameId") long gameId,
            @Param("cash") BigDecimal cash,
            @Param("debt") BigDecimal debt,
            @Param("totalSales") BigDecimal totalSales,
            @Param("cumulativeSalesProfit") BigDecimal cumulativeSalesProfit,
            @Param("totalSettlementProfit") BigDecimal totalSettlementProfit
    );

    @Select("SELECT * FROM game_player WHERE id = #{id}")
    GamePlayer findById(@Param("id") long id);
}
