package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CompanyHoldingDTO {

    private Long gamePlayerId;
    private String companyName;

    private Integer holdingPopulation;
    private BigDecimal holdingRate;

    private List<ModelHoldingDTO> models;
}
