package phonemarket.settlement;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FinancialSettlementResult {
    private List<PlayerFinancialItem> items;
    private Map<Long, PlayerFinancialItem> itemsByPlayerId;
}
