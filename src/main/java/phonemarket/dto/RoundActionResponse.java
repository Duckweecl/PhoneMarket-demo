package phonemarket.dto;

import lombok.Data;

@Data
public class RoundActionResponse {

    private Long roundId;
    private Integer roundNo;

    private Long phoneModelId;
    private Long actionId;

    private String modelName;

    private Integer submittedCount;
    private Integer expectedPlayerCount;

    private Boolean allSubmitted;
    private String roundStatus;

    private String message;
}
