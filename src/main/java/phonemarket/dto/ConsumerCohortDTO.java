package phonemarket.dto;

import lombok.Data;

@Data
public class ConsumerCohortDTO {
    private Long cohortId;
    private String segmentCode;
    private Integer population;
    private Integer usedRounds;

    private Long phoneModelId;
    private String phoneModelName;
    private String phoneModelCode;
    private Integer totalGrade;

    private Long ownerGamePlayerId;
    private String ownerCompanyName;

    private Integer screenLevel;
    private Integer processorLevel;
    private Integer bodyLevel;
    private Integer batteryLevel;
    private Integer storageLevel;
    private Integer cameraLevel;
}
