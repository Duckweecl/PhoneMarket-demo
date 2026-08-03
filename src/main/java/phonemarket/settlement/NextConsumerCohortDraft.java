package phonemarket.settlement;

import lombok.Data;

@Data
public class NextConsumerCohortDraft {
    private String segmentCode;
    private Long phoneModelId;
    private Integer population;
    private Integer usedRounds;
}
