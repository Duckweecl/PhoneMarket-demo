package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModelHoldingDTO {

    private Long phoneModelId;
    private String modelName;

    private Integer population;
    private BigDecimal holdingRate;
}
