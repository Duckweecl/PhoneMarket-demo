package phonemarket.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.dto.ComponentMarketDTO;
import phonemarket.dto.ConsumerCohortDTO;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.*;
import phonemarket.mapper.*;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementContextService {

    private static final Set<String> REQUIRED_SEGMENTS = Set.of(
            "BUSINESS_MALE", "BUSINESS_FEMALE",
            "WORKER_MALE", "WORKER_FEMALE",
            "STUDENT_MALE", "STUDENT_FEMALE"
    );
    private static final Set<String> REQUIRED_COMPONENT_TYPES = Set.of(
            "SCREEN", "PROCESSOR", "BODY", "BATTERY", "STORAGE", "CAMERA"
    );

    private final GameMapper gameMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundMapper roundMapper;
    private final RoundActionMapper roundActionMapper;
    private final PhoneModelMapper phoneModelMapper;
    private final RoundComponentMarketMapper componentMarketMapper;
    private final RoundSegmentStateMapper segmentStateMapper;
    private final RoundConsumerCohortMapper consumerCohortMapper;
    private final RoundStarMapper roundStarMapper;
    private final ConsumerSegmentRuleMapper consumerSegmentRuleMapper;

    public SettlementContext build(long gameId, long roundId) {
        Game game = gameMapper.findById(gameId);
        Round round = roundMapper.findById(roundId);
        if (game == null || round == null || !Objects.equals(round.getGameId(), gameId)) {
            throw new IllegalStateException("结算上下文组装失败：游戏或回合不存在");
        }
        if (!"PROCESSING".equals(round.getStatus())) {
            throw new IllegalStateException("结算上下文组装失败：回合状态不是PROCESSING");
        }
        if (!Objects.equals(round.getSubmittedCount(), round.getExpectedPlayerCount())) {
            throw new IllegalStateException("结算上下文组装失败：提交人数不完整");
        }

        List<GamePlayer> players = nonNull(gamePlayerMapper.findActivePlayersByGameId(gameId));
        List<RoundAction> actions = nonNull(roundActionMapper.findAllByRoundId(roundId));
        List<PhoneModel> phoneModels = nonNull(phoneModelMapper.findPlayerModelsByRoundId(roundId));
        int expected = round.getExpectedPlayerCount();
        if (players.size() != expected || actions.size() != expected || phoneModels.size() != expected) {
            throw new IllegalStateException("结算上下文组装失败：玩家、提交或手机数量不完整");
        }

        Map<Long, GamePlayer> playersById = new HashMap<>();
        for (GamePlayer player : players) {
            if (playersById.put(player.getId(), player) != null) {
                throw new IllegalStateException("结算上下文组装失败：玩家重复");
            }
        }

        Map<Long, PhoneModel> phoneModelsById = new HashMap<>();
        for (PhoneModel model : phoneModels) {
            if (!Objects.equals(model.getRoundId(), roundId)
                    || !playersById.containsKey(model.getGamePlayerId())
                    || phoneModelsById.put(model.getId(), model) != null) {
                throw new IllegalStateException("结算上下文组装失败：玩家手机关联错误");
            }
        }

        Map<Long, RoundAction> actionsByPlayerId = new HashMap<>();
        for (RoundAction action : actions) {
            PhoneModel model = phoneModelsById.get(action.getPhoneModelId());
            if (!Objects.equals(action.getRoundId(), roundId)
                    || !playersById.containsKey(action.getGamePlayerId())
                    || model == null
                    || !Objects.equals(model.getGamePlayerId(), action.getGamePlayerId())
                    || actionsByPlayerId.put(action.getGamePlayerId(), action) != null) {
                throw new IllegalStateException("结算上下文组装失败：玩家提交关联错误");
            }
        }

        List<ComponentMarketDTO> componentMarkets = componentMarketMapper.findComponentDTOsByRoundId(roundId);
        validateComponentMarkets(componentMarkets);
        List<SegmentStateDTO> segmentStates = segmentStateMapper.findSegmentDTOsByRoundId(roundId);
        validateSegments(segmentStates);
        List<ConsumerCohortDTO> cohorts = consumerCohortMapper.findCohortDTOsByRoundId(roundId);
        validateCohorts(cohorts);
        RoundStar star = roundStarMapper.findByRoundId(roundId);
        if (star == null) {
            throw new IllegalStateException("结算上下文组装失败：明星数据不存在");
        }
        List<ConsumerSegmentRule> rules = consumerSegmentRuleMapper.findAll();
        if (rules == null || rules.size() != 6) {
            throw new IllegalStateException("结算上下文组装失败：消费者规则不完整");
        }

        SettlementContext context = new SettlementContext();
        context.setGame(game);
        context.setRound(round);
        context.setPlayers(players);
        context.setActions(actions);
        context.setPhoneModels(phoneModels);
        context.setActionsByPlayerId(actionsByPlayerId);
        context.setPhoneModelsById(phoneModelsById);
        context.setComponentMarkets(componentMarkets);
        context.setSegmentStates(segmentStates);
        context.setConsumerCohorts(cohorts);
        context.setSegmentRules(rules);
        context.setStar(star);
        return context;
    }

    private void validateComponentMarkets(List<ComponentMarketDTO> markets) {
        if (markets == null || markets.size() != 18) {
            throw new IllegalStateException("结算上下文组装失败：零部件市场必须为18条");
        }
        Set<String> keys = new HashSet<>();
        for (ComponentMarketDTO market : markets) {
            if (!REQUIRED_COMPONENT_TYPES.contains(market.getComponentType())
                    || market.getComponentLevel() == null
                    || market.getComponentLevel() < 1
                    || market.getComponentLevel() > 3
                    || !keys.add(market.getComponentType() + ":" + market.getComponentLevel())) {
                throw new IllegalStateException("结算上下文组装失败：零部件市场数据错误");
            }
        }
    }

    private void validateSegments(List<SegmentStateDTO> states) {
        if (states == null || states.size() != 6) {
            throw new IllegalStateException("结算上下文组装失败：消费者状态必须为6条");
        }
        Set<String> segments = new HashSet<>();
        for (SegmentStateDTO state : states) {
            segments.add(state.getSegmentCode());
        }
        if (!segments.equals(REQUIRED_SEGMENTS)) {
            throw new IllegalStateException("结算上下文组装失败：消费者类型不完整");
        }
    }

    private void validateCohorts(List<ConsumerCohortDTO> cohorts) {
        if (cohorts == null || cohorts.isEmpty()) {
            throw new IllegalStateException("结算上下文组装失败：消费者批次不存在");
        }
        for (ConsumerCohortDTO cohort : cohorts) {
            if (!REQUIRED_SEGMENTS.contains(cohort.getSegmentCode())
                    || cohort.getPopulation() == null
                    || cohort.getPopulation() <= 0
                    || cohort.getPhoneModelId() == null
                    || cohort.getTotalGrade() == null) {
                throw new IllegalStateException("结算上下文组装失败：消费者批次数据错误");
            }
        }
    }

    private <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
