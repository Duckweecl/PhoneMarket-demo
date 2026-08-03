package phonemarket.entity;

import lombok.Data;

@Data
public class RoundSegmentState {

    private Long id;

    private Long roundId;

    private String segmentCode;

    private Integer population;

    private Integer averageBudget;
}