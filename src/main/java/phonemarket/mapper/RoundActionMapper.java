package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.dto.StarBidDTO;
import phonemarket.entity.RoundAction;

import java.util.List;

@Mapper
public interface RoundActionMapper {
    @Select("""
        SELECT * FROM round_action
        WHERE round_id = #{roundId}
        ORDER BY game_player_id
        """)
    List<RoundAction> findAllByRoundId(@Param("roundId") long roundId);

    @Select("""
        SELECT COUNT(*) FROM round_action
        WHERE round_id = #{roundId} AND game_player_id = #{gamePlayerId}
        """)
    int countByRoundAndPlayer(@Param("roundId") long roundId, @Param("gamePlayerId") long gamePlayerId);

    @Insert("""
        INSERT INTO round_action (
            round_id, game_player_id, phone_model_id,
            production_quantity, sale_price,
            film_ad, online_ad, magazine_ad, star_bid, submitted_at
        ) VALUES (
            #{roundId}, #{gamePlayerId}, #{phoneModelId},
            #{productionQuantity}, #{salePrice},
            #{filmAd}, #{onlineAd}, #{magazineAd}, #{starBid}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoundAction roundAction);

    @Select("SELECT * FROM round_action WHERE id = #{id}")
    RoundAction findById(@Param("id") long id);

    @Select("""
        SELECT ra.game_player_id,
               u.username AS company_name,
               ra.star_bid AS bid
        FROM round_action ra
        JOIN game_player gp ON gp.id = ra.game_player_id
        JOIN `user` u ON u.id = gp.user_id
        WHERE ra.round_id = #{roundId}
        ORDER BY gp.seat_no
        """)
    List<StarBidDTO> findStarBidsByRoundId(@Param("roundId") long roundId);
}
