package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.entity.Round;
import phonemarket.entity.RoundAction;

@Mapper
public interface PlayMapping {

    @Select("""
        SELECT current_round
        FROM game
        WHERE id = #{gameId}
        """)
    int getCurrentRound(long gameId);

    @Select("""
    SELECT id
    FROM game_round
    WHERE game_id = #{gameId}
      AND round_no = #{roundNo}
    """)
    Long getRoundId(
            @Param("gameId") long gameId,
            @Param("roundNo") int roundNo
    );


    @Select("""
        SELECT id
        FROM game_round
        WHERE game_id = #{gameId}
          AND round_no = #{roundNo}
        """)
    Long findRoundId(
            @Param("gameId") long gameId,
            @Param("roundNo") int roundNo
    );

    @Insert("""
        INSERT INTO round_action
        (round_id, game_player_id, action_type)
        VALUES
        (#{roundId}, #{gamePlayerId}, #{actionType})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int submitAction(RoundAction roundAction);


    @Update("""
        UPDATE game_round
        SET submitted_count = submitted_count + 1
        WHERE id = #{roundId}
        """)
    @Options
    int increaseSubmittedCount(long roundId);

    @Update("""
    UPDATE game_round
    SET status = 'FINISHED',
        ended_at = CURRENT_TIMESTAMP
    WHERE id = #{roundId}
    """)
    int endRound(long roundId);

    @Select("""
        SELECT *
        FROM round_action
        WHERE id = #{id}
        """)
    @Options
    RoundAction findRoundAction(long id);

    @Select("""
        SELECT user_id
        FROM game_player
        WHERE game_id = #{gameId}
        AND user_id = #{userid}
        """)
    @Options
    Long findGamePlayerId(long userid,long gameId);

    @Select("""
        SELECT *
        FROM game_round
        WHERE id = #{roundId}
        """)
    @Options
    Round getRound(long roundno);


}