package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
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
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Game game);

    @Select("""
        SELECT *
        FROM game
        WHERE id = #{id}
    """)
    @Options
    Game Findbygame(long id);


    @Insert("""
            INSERT INTO game_player
            (game_id, user_id, seat_no)
            VALUES 
            (#{gameId}, #{userId}, #{seatNo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int join(GamePlayer player);

    @Update("""
    UPDATE game
    SET player_count = player_count + 1
    WHERE id = #{gameId}
    AND player_count < 4
    """)
    @Options
    int increasePlayerCount(Long gameId);

    @Update("""
    UPDATE game
    SET current_round = current_round + 1
    WHERE id = #{gameId}
    AND current_round <= max_round
    """)
    @Options
    int increaseCurrentRound(Long gameId);

    @Update("""
    UPDATE game
    SET player_count = player_count - 1
    WHERE id = #{gameId}
    AND player_count < 4
    """)
    int decreasePlayerCount(Long gameId);

    @Select("""
            SELECT *
            FROM game_player
            WHERE id = #{id}
            """)
    @Options
    GamePlayer findPlayer(long id);




    @Select("""
    SELECT COUNT(*)
    FROM game_player
    WHERE user_id = #{userId}
      AND game_id = #{gameId}
    """)
    int countPlayer(
            @Param("userId") Long userId,
            @Param("gameId") Long gameId
    );



    @Update("""
    UPDATE game
    SET status = "FINISHED"
    WHERE id = #{gameId}
    """)
    int endGame(Long gameId);

    @Update("""
    UPDATE game
    SET status = "ABORTED"
    WHERE id = #{gameId}
    """)
    int abortGame(Long gameId);

    @Update("""
    UPDATE game_player
    SET status = "INACTIVE"
    WHERE game_id = #{gameId}
    """)
    int playersdismiss(Long gameId);

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
    List<Integer> findActiveSeatNumbers(long gameId);

    @Select("""
    SELECT *
    FROM game_player
    WHERE game_id = #{gameId}
      AND status = 'ACTIVE'
    ORDER BY seat_no ASC
    """)
    List<GamePlayer> findPlayersInRoom(long gameId);
    @Insert("""
    INSERT INTO game_round
    (game_id, round_no, expected_player_count)
    VALUES
    (#{gameId},#{roundNo},#{expectedPlayerCount})
""")
    @Options
    int newRound (Round round);
    @Select("""
        SELECT *
        FROM game
        WHERE id = #{gameId}
        """)
    Game findById(long gameId);

    @Select("""
    SELECT user_id
    FROM game_player
    WHERE game_id = #{gameId}
      AND seat_no = 1
      AND status = 'ACTIVE'
    LIMIT 1
    """)
    Long findOwnerUserId(@Param("gameId") long gameId);

    @Update("""
        UPDATE game
        SET status = 'RUNNING',
            current_round = 1,
            player_count = #{playerCount}
        WHERE id = #{gameId}
          AND status = 'WAITING'
        """)
    int startGame(
            @Param("gameId") long gameId,
            @Param("playerCount") int playerCount
    );

    @Update("""
        UPDATE game
        SET current_round = #{roundNo}
        WHERE id = #{gameId} AND status = 'RUNNING'
        """)
    int setCurrentRound(@Param("gameId") long gameId, @Param("roundNo") int roundNo);

    @Update("""
        UPDATE game
        SET status = 'FINISHED'
        WHERE id = #{gameId} AND status = 'RUNNING'
        """)
    int finishRunningGame(@Param("gameId") long gameId);
}
