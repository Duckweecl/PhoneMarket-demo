package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.*;
import phonemarket.mapper.*;
import phonemarket.settlement.ComponentMarketSettlementItem;
import phonemarket.settlement.ComponentMarketSettlementResult;
import phonemarket.settlement.NextConsumerCohortDraft;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoundInitializationService {

    private static final List<String> COMPONENT_TYPES = List.of(
            "SCREEN", "PROCESSOR", "BODY", "BATTERY", "STORAGE", "CAMERA"
    );

    private final RoundMapper roundMapper;
    private final ConsumerSegmentRuleMapper consumerSegmentRuleMapper;
    private final RoundSegmentStateMapper roundSegmentStateMapper;
    private final RoundComponentMarketMapper roundComponentMarketMapper;
    private final RoundStarMapper roundStarMapper;
    private final PhoneModelMapper phoneModelMapper;
    private final RoundConsumerCohortMapper roundConsumerCohortMapper;

    public Round initializeFirstRound(long gameId, int playerCount) {
        List<ConsumerSegmentRule> rules = loadRules();
        Map<String, PhoneModel> systemModels = loadSystemModels();

        Round round = new Round();
        round.setGameId(gameId);
        round.setRoundNo(1);
        round.setStatus("COLLECTING");
        round.setExpectedPlayerCount(playerCount);
        round.setSubmittedCount(0);
        round.setEconomyFactor(BigDecimal.ZERO.setScale(4));
        insertRound(round);

        List<RoundSegmentState> states = new ArrayList<>();
        List<RoundConsumerCohort> cohorts = new ArrayList<>();
        for (ConsumerSegmentRule rule : rules) {
            int population = rule.getBasePopulation() * playerCount;
            RoundSegmentState state = new RoundSegmentState();
            state.setRoundId(round.getId());
            state.setSegmentCode(rule.getSegmentCode());
            state.setPopulation(population);
            state.setAverageBudget(rule.getInitialBudget());
            states.add(state);

            PhoneModel systemModel = systemModels.get(getInitialModelCode(rule.getGroupType()));
            RoundConsumerCohort cohort = new RoundConsumerCohort();
            cohort.setRoundId(round.getId());
            cohort.setSegmentCode(rule.getSegmentCode());
            cohort.setPhoneModelId(systemModel.getId());
            cohort.setPopulation(population);
            cohort.setUsedRounds(rule.getInitialUsedRounds());
            cohorts.add(cohort);
        }
        roundSegmentStateMapper.batchInsert(states);
        roundComponentMarketMapper.batchInsert(createInitialComponentMarkets(round.getId(), playerCount));
        roundStarMapper.insert(createRoundStar(round.getId(), rules));
        roundConsumerCohortMapper.batchInsert(cohorts);
        return round;
    }

    public Round initializeNextRound(
            long gameId,
            Round previousRound,
            int playerCount,
            List<SegmentStateDTO> previousStates,
            List<NextConsumerCohortDraft> nextCohortDrafts,
            ComponentMarketSettlementResult componentResult
    ) {
        List<ConsumerSegmentRule> rules = loadRules();
        Map<String, ConsumerSegmentRule> rulesBySegment = rules.stream()
                .collect(Collectors.toMap(ConsumerSegmentRule::getSegmentCode, Function.identity()));
        BigDecimal economyFactor = generateEconomyFactor();

        Round nextRound = new Round();
        nextRound.setGameId(gameId);
        nextRound.setRoundNo(previousRound.getRoundNo() + 1);
        nextRound.setStatus("COLLECTING");
        nextRound.setExpectedPlayerCount(playerCount);
        nextRound.setSubmittedCount(0);
        nextRound.setEconomyFactor(economyFactor);
        insertRound(nextRound);

        List<RoundSegmentState> nextStates = new ArrayList<>();
        for (SegmentStateDTO previous : previousStates) {
            ConsumerSegmentRule rule = rulesBySegment.get(previous.getSegmentCode());
            if (rule == null) {
                throw new IllegalStateException("下一回合创建失败：缺少消费者规则 " + previous.getSegmentCode());
            }
            BigDecimal change = BigDecimal.valueOf(rule.getBudgetGrowth())
                    .multiply(rule.getEconomySensitivity())
                    .multiply(economyFactor)
                    .setScale(0, RoundingMode.HALF_UP);
            int nextBudget = BigDecimal.valueOf(previous.getAverageBudget())
                    .add(change)
                    .max(BigDecimal.ZERO)
                    .intValueExact();

            RoundSegmentState state = new RoundSegmentState();
            state.setRoundId(nextRound.getId());
            state.setSegmentCode(previous.getSegmentCode());
            state.setPopulation(previous.getPopulation());
            state.setAverageBudget(nextBudget);
            nextStates.add(state);
        }
        roundSegmentStateMapper.batchInsert(nextStates);

        List<RoundConsumerCohort> cohorts = new ArrayList<>();
        for (NextConsumerCohortDraft draft : nextCohortDrafts) {
            if (draft.getPopulation() == null || draft.getPopulation() <= 0) continue;
            RoundConsumerCohort cohort = new RoundConsumerCohort();
            cohort.setRoundId(nextRound.getId());
            cohort.setSegmentCode(draft.getSegmentCode());
            cohort.setPhoneModelId(draft.getPhoneModelId());
            cohort.setPopulation(draft.getPopulation());
            cohort.setUsedRounds(draft.getUsedRounds());
            cohorts.add(cohort);
        }
        validatePopulation(previousStates, cohorts);
        roundConsumerCohortMapper.batchInsert(cohorts);

        List<RoundComponentMarket> nextMarkets = new ArrayList<>();
        for (ComponentMarketSettlementItem item : componentResult.getItems()) {
            int shortage = Math.max(0, item.getDemandQuantity() - item.getSupplyQuantity());
            int expansion = (int) Math.ceil(shortage * 0.5d);
            int nextSupply = item.getSupplyQuantity() + expansion;
            int updated = roundComponentMarketMapper.updateNextSupply(
                    previousRound.getId(), item.getComponentType(), item.getComponentLevel(), nextSupply
            );
            if (updated != 1) {
                throw new IllegalStateException("下一回合创建失败：无法保存下一回合供货量");
            }

            RoundComponentMarket market = new RoundComponentMarket();
            market.setRoundId(nextRound.getId());
            market.setComponentType(item.getComponentType());
            market.setComponentLevel(item.getComponentLevel());
            market.setBasePrice(item.getBaseUnitPrice().intValue());
            market.setSupplyQuantity(nextSupply);
            market.setDemandQuantity(0);
            market.setPremiumFactor(item.getNewPremiumFactor());
            market.setActualUnitPrice(item.getBaseUnitPrice()
                    .multiply(item.getNewPremiumFactor())
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact());
            market.setNextSupplyQuantity(null);
            nextMarkets.add(market);
        }
        roundComponentMarketMapper.batchInsert(nextMarkets);
        roundStarMapper.insert(createRoundStar(nextRound.getId(), rules));
        return nextRound;
    }

    private void validatePopulation(List<SegmentStateDTO> states, List<RoundConsumerCohort> cohorts) {
        Map<String, Integer> expected = states.stream().collect(Collectors.toMap(
                SegmentStateDTO::getSegmentCode, SegmentStateDTO::getPopulation
        ));
        Map<String, Integer> actual = new HashMap<>();
        for (RoundConsumerCohort cohort : cohorts) {
            actual.merge(cohort.getSegmentCode(), cohort.getPopulation(), Integer::sum);
        }
        if (!expected.equals(actual)) {
            throw new IllegalStateException("下一回合创建失败：消费者批次人口与人群人口不一致，expected="
                    + expected + "，actual=" + actual);
        }
    }

    private void insertRound(Round round) {
        if (roundMapper.insertRound(round) != 1 || round.getId() == null) {
            throw new IllegalStateException("回合创建失败");
        }
    }

    private BigDecimal generateEconomyFactor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean positive = random.nextDouble() < 0.75;
        double magnitude = positive
                ? random.nextDouble(0.65, 1.150001)
                : random.nextDouble(0.05, 0.950001);
        return BigDecimal.valueOf(positive ? magnitude : -magnitude)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private RoundStar createRoundStar(long roundId, List<ConsumerSegmentRule> rules) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ConsumerSegmentRule target = rules.get(random.nextInt(rules.size()));
        BigDecimal boost = BigDecimal.valueOf(random.nextDouble(0, 0.500001))
                .setScale(2, RoundingMode.HALF_UP);
        RoundStar star = new RoundStar();
        star.setRoundId(roundId);
        star.setTargetSegmentCode(target.getSegmentCode());
        star.setBoost(boost);
        return star;
    }

    private List<RoundComponentMarket> createInitialComponentMarkets(long roundId, int playerCount) {
        List<RoundComponentMarket> result = new ArrayList<>();
        for (String type : COMPONENT_TYPES) {
            for (int level = 1; level <= 3; level++) {
                int basePrice = switch (level) {
                    case 1 -> 150;
                    case 2 -> 250;
                    case 3 -> 450;
                    default -> throw new IllegalArgumentException();
                };
                int supply = switch (level) {
                    case 1 -> playerCount * 1000;
                    case 2 -> playerCount * 750;
                    case 3 -> playerCount * 250;
                    default -> throw new IllegalArgumentException();
                };
                RoundComponentMarket market = new RoundComponentMarket();
                market.setRoundId(roundId);
                market.setComponentType(type);
                market.setComponentLevel(level);
                market.setBasePrice(basePrice);
                market.setSupplyQuantity(supply);
                market.setDemandQuantity(0);
                market.setPremiumFactor(new BigDecimal("1.0000"));
                market.setActualUnitPrice(basePrice);
                market.setNextSupplyQuantity(null);
                result.add(market);
            }
        }
        return result;
    }

    private List<ConsumerSegmentRule> loadRules() {
        List<ConsumerSegmentRule> rules = consumerSegmentRuleMapper.findAll();
        if (rules == null || rules.size() != 6) {
            throw new IllegalStateException("消费者规则数据不完整，应存在6条记录");
        }
        return rules;
    }

    private Map<String, PhoneModel> loadSystemModels() {
        Map<String, PhoneModel> result = phoneModelMapper.findSystemModels().stream()
                .collect(Collectors.toMap(PhoneModel::getModelCode, Function.identity()));
        for (String code : List.of("INITIAL_BUSINESS", "INITIAL_WORKER", "INITIAL_STUDENT")) {
            if (!result.containsKey(code)) {
                throw new IllegalStateException("缺少系统初始手机：" + code);
            }
        }
        return result;
    }

    private String getInitialModelCode(String groupType) {
        return switch (groupType) {
            case "BUSINESS" -> "INITIAL_BUSINESS";
            case "WORKER" -> "INITIAL_WORKER";
            case "STUDENT" -> "INITIAL_STUDENT";
            default -> throw new IllegalStateException("未知消费者类型：" + groupType);
        };
    }
}
