package phonemarket.settlement;

import lombok.Data;
import phonemarket.dto.ComponentMarketDTO;
import phonemarket.dto.ConsumerCohortDTO;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.ConsumerSegmentRule;
import phonemarket.entity.Game;
import phonemarket.entity.GamePlayer;
import phonemarket.entity.PhoneModel;
import phonemarket.entity.Round;
import phonemarket.entity.RoundAction;
import phonemarket.entity.RoundStar;

import java.util.List;
import java.util.Map;

@Data
public class SettlementContext {

    private Game game;
    private Round round;

    private List<GamePlayer> players;
    private List<RoundAction> actions;
    private List<PhoneModel> phoneModels;

    private Map<Long, RoundAction> actionsByPlayerId;
    private Map<Long, PhoneModel> phoneModelsById;

    private List<ComponentMarketDTO> componentMarkets;
    private List<SegmentStateDTO> segmentStates;
    private List<ConsumerCohortDTO> consumerCohorts;
    private List<ConsumerSegmentRule> segmentRules;

    private RoundStar star;
}