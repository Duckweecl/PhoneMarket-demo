package phonemarket.mapper;

import org.apache.ibatis.annotations.*;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.RoundSegmentState;

import java.util.List;

@Mapper
public interface RoundSegmentStateMapper {
    @Select("SELECT COALESCE(SUM(population), 0) FROM round_segment_state WHERE round_id = #{roundId}")
    int sumPopulationByRoundId(@Param("roundId") long roundId);

    @Insert("""
        <script>
        INSERT INTO round_segment_state (round_id, segment_code, population, average_budget)
        VALUES
        <foreach collection="states" item="state" separator=",">
            (#{state.roundId}, #{state.segmentCode}, #{state.population}, #{state.averageBudget})
        </foreach>
        </script>
        """)
    int batchInsert(@Param("states") List<RoundSegmentState> states);

    @Select("""
        SELECT segment_code, population, average_budget
        FROM round_segment_state
        WHERE round_id = #{roundId}
        ORDER BY segment_code
        """)
    List<SegmentStateDTO> findSegmentDTOsByRoundId(@Param("roundId") long roundId);
}
