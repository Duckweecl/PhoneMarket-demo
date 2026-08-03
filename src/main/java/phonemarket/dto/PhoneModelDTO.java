package phonemarket.dto;

import lombok.Data;

@Data
public class PhoneModelDTO {

    private Long phoneModelId;
    private String modelName;

    private Integer screenLevel;
    private Integer processorLevel;
    private Integer bodyLevel;
    private Integer batteryLevel;
    private Integer storageLevel;
    private Integer cameraLevel;

    private Integer totalGrade;
}
