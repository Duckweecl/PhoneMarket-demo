package phonemarket.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsumerSegmentRule {

    private String segmentCode;

    private String groupType;
    private String gender;

    private Integer basePopulation;

    private Integer initialBudget;
    private Integer budgetGrowth;
    private BigDecimal economySensitivity;

    private BigDecimal screenPreference;
    private BigDecimal processorPreference;
    private BigDecimal bodyPreference;
    private BigDecimal batteryPreference;
    private BigDecimal storagePreference;
    private BigDecimal cameraPreference;

    private Integer initialScreenLevel;
    private Integer initialProcessorLevel;
    private Integer initialBodyLevel;
    private Integer initialBatteryLevel;
    private Integer initialStorageLevel;
    private Integer initialCameraLevel;

    private Integer initialUsedRounds;
}