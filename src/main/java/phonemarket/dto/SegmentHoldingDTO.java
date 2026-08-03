package phonemarket.dto;

import lombok.Data;

import java.util.List;

@Data
public class SegmentHoldingDTO {

    private String segmentCode;
    private Integer totalPopulation;

    private List<CompanyHoldingDTO> companyHoldings;
}
