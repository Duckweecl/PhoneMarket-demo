package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.dto.ConsumerCohortDTO;
import phonemarket.entity.RoundConsumerCohort;

import java.util.List;

@Mapper
public interface RoundConsumerCohortMapper {
    @Insert("""
        <script>
        INSERT INTO round_consumer_cohort (
            round_id, segment_code, phone_model_id, population, used_rounds
        ) VALUES
        <foreach collection="cohorts" item="cohort" separator=",">
            (#{cohort.roundId}, #{cohort.segmentCode}, #{cohort.phoneModelId},
             #{cohort.population}, #{cohort.usedRounds})
        </foreach>
        </script>
        """)
    int batchInsert(@Param("cohorts") List<RoundConsumerCohort> cohorts);

    @Select("""
        SELECT
            cohort.id AS cohort_id,
            cohort.segment_code,
            cohort.population,
            cohort.used_rounds,
            model.id AS phone_model_id,
            model.model_name AS phone_model_name,
            model.model_code AS phone_model_code,
            model.total_grade,
            model.game_player_id AS owner_game_player_id,
            u.username AS owner_company_name,
            model.screen_level,
            model.processor_level,
            model.body_level,
            model.battery_level,
            model.storage_level,
            model.camera_level
        FROM round_consumer_cohort cohort
        JOIN phone_model model ON model.id = cohort.phone_model_id
        LEFT JOIN game_player player ON player.id = model.game_player_id
        LEFT JOIN `user` u ON u.id = player.user_id
        WHERE cohort.round_id = #{roundId}
        ORDER BY cohort.segment_code, cohort.id
        """)
    List<ConsumerCohortDTO> findCohortDTOsByRoundId(@Param("roundId") long roundId);
}
