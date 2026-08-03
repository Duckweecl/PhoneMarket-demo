package phonemarket.settlement;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ConsumerPurchaseSettlementResult {
    private List<PlayerSalesItem> playerSalesItems;
    private Map<Long, PlayerSalesItem> playerSalesByPlayerId;
    private List<ConsumerPurchaseDetail> purchaseDetails;
    private List<NextConsumerCohortDraft> nextCohorts;
    private Integer unservedPopulation;

    public PlayerSalesItem getByPlayerId(long gamePlayerId) {
        PlayerSalesItem item = playerSalesByPlayerId.get(gamePlayerId);
        if (item == null) {
            throw new IllegalArgumentException("找不到玩家销售结果，gamePlayerId=" + gamePlayerId);
        }
        return item;
    }
}
