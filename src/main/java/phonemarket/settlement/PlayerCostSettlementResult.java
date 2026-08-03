package phonemarket.settlement;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PlayerCostSettlementResult {

    private Long totalPopulation;

    private List<PlayerCostItem> items;


    private Map<Long, PlayerCostItem> itemsByPlayerId;

    public PlayerCostItem getByPlayerId(
            long gamePlayerId
    ) {
        PlayerCostItem item =
                itemsByPlayerId.get(
                        gamePlayerId
                );

        if (item == null) {
            throw new IllegalArgumentException(
                    "找不到玩家成本结果，gamePlayerId="
                            + gamePlayerId
            );
        }

        return item;
    }
}