package phonemarket.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import phonemarket.entity.ConsumerSegmentRule;

import java.util.List;

@Mapper
public interface ConsumerSegmentRuleMapper {

    @Select("""
        SELECT *
        FROM consumer_segment_rule
        ORDER BY segment_code
        """)
    List<ConsumerSegmentRule> findAll();
}