package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.Round;

@Mapper
public interface RoundMapper {
    @Update("""
        UPDATE game_round
        SET status = 'PROCESSING'
        WHERE id = #{roundId}
          AND status = 'COLLECTING'
          AND submitted_count = expected_player_count
        """)
    int claimSettlement(@Param("roundId") long roundId);

    @Insert("""
        INSERT INTO game_round (
            game_id, round_no, status, expected_player_count,
            submitted_count, economy_factor, started_at
        ) VALUES (
            #{gameId}, #{roundNo}, #{status}, #{expectedPlayerCount},
            #{submittedCount}, #{economyFactor}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRound(Round round);

    @Select("""
        SELECT * FROM game_round
        WHERE game_id = #{gameId} AND round_no = #{roundNo}
        LIMIT 1
        """)
    Round findByGameAndRoundNo(@Param("gameId") long gameId, @Param("roundNo") int roundNo);

    @Select("SELECT * FROM game_round WHERE id = #{roundId}")
    Round findById(@Param("roundId") long roundId);

    @Select("SELECT * FROM game_round WHERE id = #{roundId} FOR UPDATE")
    Round findByIdForUpdate(@Param("roundId") long roundId);

    @Update("""
        UPDATE game_round
        SET submitted_count = submitted_count + 1
        WHERE id = #{roundId}
          AND status = 'COLLECTING'
          AND submitted_count < expected_player_count
        """)
    int increaseSubmittedCount(@Param("roundId") long roundId);

    @Update("""
        UPDATE game_round
        SET status = 'FINISHED', ended_at = CURRENT_TIMESTAMP
        WHERE id = #{roundId} AND status = 'PROCESSING'
        """)
    int finishRound(@Param("roundId") long roundId);
}
