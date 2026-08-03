package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.RoundStar;

import java.math.BigDecimal;

@Mapper
public interface RoundStarMapper {
    @Insert("""
        INSERT INTO round_star (
            round_id, target_segment_code, boost,
            winner_game_player_id, winning_bid
        ) VALUES (
            #{roundId}, #{targetSegmentCode}, #{boost}, NULL, NULL
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoundStar roundStar);

    @Select("SELECT * FROM round_star WHERE round_id = #{roundId} LIMIT 1")
    RoundStar findByRoundId(@Param("roundId") long roundId);

    @Update("""
        UPDATE round_star
        SET winner_game_player_id = #{winnerGamePlayerId}, winning_bid = #{winningBid}
        WHERE round_id = #{roundId} AND winning_bid IS NULL
        """)
    int settleWithWinner(
            @Param("roundId") long roundId,
            @Param("winnerGamePlayerId") long winnerGamePlayerId,
            @Param("winningBid") BigDecimal winningBid
    );

    @Update("""
        UPDATE round_star
        SET winner_game_player_id = NULL, winning_bid = 0
        WHERE round_id = #{roundId} AND winning_bid IS NULL
        """)
    int settleWithoutWinner(@Param("roundId") long roundId);
}
