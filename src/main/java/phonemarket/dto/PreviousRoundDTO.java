package phonemarket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PreviousRoundDTO {
    private Long roundId;
    private Integer roundNo;
    private BigDecimal economyFactor;
    private List<ComponentMarketDTO> componentMarkets;
    private StarResultDTO starResult;
    private List<PlayerRoundResultDTO> playerResults;
    private PlayerFinancialResultDTO currentPlayerFinancialResult;
}
