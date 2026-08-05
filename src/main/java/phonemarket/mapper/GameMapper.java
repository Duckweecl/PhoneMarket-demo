package phonemarket.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import phonemarket.dto.ActiveGameItem;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;
import phonemarket.entity.Round;

import java.util.List;

@Mapper
public interface GameMapper {

    @Insert("""
        INSERT INTO game
            (status, current_round, max_round, player_count)
        VALUES
            (#{status}, #{currentRound}, #{maxRound}, #{playerCount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Game game);

    @Select("""
        SELECT
            id,
            status,
            current_round AS currentRound,
            max_round AS maxRound,
            player_count AS playerCount,
            created_at AS createdAt,
            started_at AS startedAt,
            last_activity_at AS lastActivityAt,
            finished_at AS finishedAt,
            finished_reason AS finishedReason
        FROM game
        WHERE id = #{id}
        """)
    Game Findbygame(@Param("id") long id);

    @Select("""
        SELECT
            id,
            status,
            current_round AS currentRound,
            max_round AS maxRound,
            player_count AS playerCount,
            created_at AS createdAt,
            started_at AS startedAt,
            last_activity_at AS lastActivityAt,
            finished_at AS finishedAt,
            finished_reason AS finishedReason
        FROM game
        WHERE id = #{gameId}
        """)
    Game findById(@Param("gameId") long gameId);

    @Insert("""
        INSERT INTO game_player
            (game_id, user_id, seat_no)
        VALUES
            (#{gameId}, #{userId}, #{seatNo})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int join(GamePlayer player);

    @Select("""
        SELECT
            gp.id,
            gp.game_id AS gameId,
            gp.user_id AS userId,
            gp.seat_no AS seatNo,
            gp.cash,
            gp.debt,
            gp.debt_limit AS debtLimit,
            gp.total_sales AS totalSales,
            gp.cumulative_sales_profit AS cumulativeSalesProfit,
            gp.total_settlement_profit AS totalSettlementProfit,
            gp.status,
            gp.joined_at AS joinedAt,
            u.username,
            u.nickname
        FROM game_player gp
        JOIN `user` u ON u.id = gp.user_id
        WHERE gp.game_id = #{gameId}
        ORDER BY gp.seat_no ASC
        """)
    List<GamePlayer> findPlayersInRoom(@Param("gameId") long gameId);

    @Select("""
        SELECT
            id,
            game_id AS gameId,
            user_id AS userId,
            seat_no AS seatNo,
            cash,
            debt,
            debt_limit AS debtLimit,
            total_sales AS totalSales,
            cumulative_sales_profit AS cumulativeSalesProfit,
            total_settlement_profit AS totalSettlementProfit,
            status,
            joined_at AS joinedAt
        FROM game_player
        WHERE id = #{id}
        """)
    GamePlayer findPlayer(@Param("id") long id);

    @Select("""
        SELECT COUNT(*)
        FROM game_player
        WHERE user_id = #{userId}
          AND game_id = #{gameId}
          AND status = 'ACTIVE'
        """)
    int countPlayer(
            @Param("userId") long userId,
            @Param("gameId") long gameId
    );

    /**
     * 包括 INACTIVE，用于已解散房间向原成员返回解散通知。
     */
    @Select("""
        SELECT COUNT(*)
        FROM game_player
        WHERE user_id = #{userId}
          AND game_id = #{gameId}
        """)
    int countMembership(
            @Param("userId") long userId,
            @Param("gameId") long gameId
    );

    @Select("""
        SELECT
            id,
            game_id AS gameId,
            user_id AS userId,
            seat_no AS seatNo,
            status,
            joined_at AS joinedAt
        FROM game_player
        WHERE user_id = #{userId}
          AND game_id = #{gameId}
          AND status = 'ACTIVE'
        LIMIT 1
        """)
    GamePlayer findActivePlayer(
            @Param("userId") long userId,
            @Param("gameId") long gameId
    );

    @Update("""
        UPDATE game
        SET player_count = player_count + 1
        WHERE id = #{gameId}
          AND player_count < 4
        """)
    int increasePlayerCount(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET player_count = player_count - 1
        WHERE id = #{gameId}
          AND player_count > 0
        """)
    int decreasePlayerCount(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET current_round = current_round + 1
        WHERE id = #{gameId}
          AND current_round <= max_round
        """)
    int increaseCurrentRound(@Param("gameId") long gameId);

    @Delete("""
        DELETE FROM game_player
        WHERE user_id = #{userId}
          AND game_id = #{gameId}
          AND status = 'ACTIVE'
        """)
    int playerLeft(
            @Param("userId") long userId,
            @Param("gameId") long gameId
    );

    @Select("""
        SELECT seat_no
        FROM game_player
        WHERE game_id = #{gameId}
          AND status = 'ACTIVE'
        ORDER BY seat_no
        """)
    List<Integer> findActiveSeatNumbers(@Param("gameId") long gameId);

    @Select("""
        SELECT user_id
        FROM game_player
        WHERE game_id = #{gameId}
          AND seat_no = 1
        LIMIT 1
        """)
    Long findOwnerUserId(@Param("gameId") long gameId);

    @Select("""
        SELECT g.id
        FROM game g
        JOIN game_player gp
          ON gp.game_id = g.id
         AND gp.seat_no = 1
        WHERE gp.user_id = #{userId}
          AND g.status = 'WAITING'
        ORDER BY g.id
        """)
    List<Long> findWaitingGameIdsOwnedByUser(@Param("userId") long userId);

    @Update("""
        UPDATE game
        SET status = 'ABORTED',
            finished_at = CURRENT_TIMESTAMP,
            finished_reason = 'OWNER_ABORTED'
        WHERE id = #{gameId}
          AND status = 'WAITING'
        """)
    int abortGame(@Param("gameId") long gameId);

    @Update("""
        UPDATE game_player
        SET status = 'INACTIVE'
        WHERE game_id = #{gameId}
          AND status = 'ACTIVE'
        """)
    int playersdismiss(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET status = 'RUNNING',
            current_round = 1,
            player_count = #{playerCount},
            started_at = CURRENT_TIMESTAMP,
            last_activity_at = CURRENT_TIMESTAMP,
            finished_at = NULL,
            finished_reason = NULL
        WHERE id = #{gameId}
          AND status = 'WAITING'
        """)
    int startGame(
            @Param("gameId") long gameId,
            @Param("playerCount") int playerCount
    );

    @Update("""
        UPDATE game
        SET last_activity_at = CURRENT_TIMESTAMP
        WHERE id = #{gameId}
          AND status = 'RUNNING'
        """)
    int touchActivity(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET status = 'FINISHED',
            finished_at = CURRENT_TIMESTAMP,
            finished_reason = 'INACTIVITY_TIMEOUT'
        WHERE status = 'RUNNING'
          AND COALESCE(last_activity_at, started_at, created_at)
              < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 MINUTE)
        """)
    int expireInactiveRunningGames();

    @Update("""
        UPDATE game
        SET status = 'FINISHED',
            finished_at = CURRENT_TIMESTAMP,
            finished_reason = 'NORMAL'
        WHERE id = #{gameId}
        """)
    int endGame(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET current_round = #{roundNo}
        WHERE id = #{gameId}
          AND status = 'RUNNING'
        """)
    int setCurrentRound(
            @Param("gameId") long gameId,
            @Param("roundNo") int roundNo
    );

    @Update("""
        UPDATE game
        SET status = 'FINISHED',
            finished_at = CURRENT_TIMESTAMP,
            finished_reason = 'NORMAL'
        WHERE id = #{gameId}
          AND status = 'RUNNING'
        """)
    int finishRunningGame(@Param("gameId") long gameId);

    @Insert("""
        INSERT INTO game_round
            (game_id, round_no, expected_player_count)
        VALUES
            (#{gameId}, #{roundNo}, #{expectedPlayerCount})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int newRound(Round round);

    @Select("""
        SELECT
            g.id AS gameId,
            g.status,
            g.current_round AS currentRound,
            g.max_round AS maxRound,
            g.player_count AS playerCount,
            gp.seat_no AS seatNo,
            CASE WHEN gp.seat_no = 1 THEN TRUE ELSE FALSE END AS owner,
            g.created_at AS createdAt,
            g.last_activity_at AS lastActivityAt
        FROM game_player gp
        JOIN game g ON g.id = gp.game_id
        WHERE gp.user_id = #{userId}
          AND gp.status = 'ACTIVE'
          AND g.status IN ('WAITING', 'RUNNING')
        ORDER BY
            CASE WHEN g.status = 'RUNNING' THEN 0 ELSE 1 END,
            COALESCE(g.last_activity_at, g.created_at) DESC,
            g.id DESC
        """)
    List<ActiveGameItem> findActiveGamesForUser(@Param("userId") long userId);
}
