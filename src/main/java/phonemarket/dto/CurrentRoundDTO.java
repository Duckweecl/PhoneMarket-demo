package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CurrentRoundDTO {
    private Long roundId;
    private Integer roundNo;
    private String status;
    private BigDecimal economyFactor;
    private List<SegmentStateDTO> segments;
    private List<ConsumerCohortDTO> consumerCohorts;
    private List<SegmentHoldingDTO> segmentHoldings;
    private List<ComponentMarketDTO> componentMarkets;
    private StarDTO star;
}
