package phonemarket.entity;

import lombok.Data;

@Data
public class RoundConsumerCohort {

    private Long id;

    private Long roundId;

    private String segmentCode;

    private Long phoneModelId;

    private Integer population;

    private Integer usedRounds;
}