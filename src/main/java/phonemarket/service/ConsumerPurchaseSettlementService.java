package phonemarket.service;

import org.springframework.stereotype.Service;
import phonemarket.dto.ConsumerCohortDTO;
import phonemarket.dto.SegmentStateDTO;
import phonemarket.entity.ConsumerSegmentRule;
import phonemarket.entity.PhoneModel;
import phonemarket.entity.RoundAction;
import phonemarket.settlement.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsumerPurchaseSettlementService {

    private static final List<String> SEGMENT_ORDER = List.of(
            "BUSINESS_FEMALE", "BUSINESS_MALE",
            "WORKER_FEMALE", "WORKER_MALE",
            "STUDENT_FEMALE", "STUDENT_MALE"
    );
    private static final BigDecimal PERFORMANCE_THRESHOLD = new BigDecimal("3.000000");
    private static final BigDecimal COMPONENT_GAP_FACTOR = new BigDecimal("0.25");
    private static final BigDecimal AD_BOOST = new BigDecimal("0.20");
    private static final BigDecimal BRAND_SHARE_FACTOR = new BigDecimal("0.50");
    private static final BigDecimal PRICE_CEILING_FACTOR = new BigDecimal("1.25");
    private static final BigDecimal MIN_PRICE_SCORE = new BigDecimal("0.01");
    private static final BigDecimal LIQUIDATION_RATE = new BigDecimal("0.85");
    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

    public ConsumerPurchaseSettlementResult settle(
            SettlementContext context,
            PlayerCostSettlementResult costResult,
            StarSettlementResult starResult
    ) {
        validate(context, costResult, starResult);

        Map<String, SegmentStateDTO> stateBySegment = context.getSegmentStates().stream()
                .collect(Collectors.toMap(SegmentStateDTO::getSegmentCode, Function.identity()));
        Map<String, ConsumerSegmentRule> ruleBySegment = context.getSegmentRules().stream()
                .collect(Collectors.toMap(ConsumerSegmentRule::getSegmentCode, Function.identity()));
        Map<String, Map<Long, BigDecimal>> brandShares = buildBrandShareSnapshot(context.getConsumerCohorts());

        Map<Long, Integer> remainingInventory = new LinkedHashMap<>();
        Map<Long, Integer> salesByPlayer = new LinkedHashMap<>();
        for (RoundAction action : context.getActions()) {
            remainingInventory.put(action.getGamePlayerId(), action.getProductionQuantity());
            salesByPlayer.put(action.getGamePlayerId(), 0);
        }

        List<ConsumerPurchaseDetail> details = new ArrayList<>();
        Map<String, NextConsumerCohortDraft> nextCohortMap = new LinkedHashMap<>();
        int unservedPopulation = 0;

        List<ConsumerCohortDTO> orderedCohorts = new ArrayList<>(context.getConsumerCohorts());
        orderedCohorts.sort(
                Comparator.comparingInt((ConsumerCohortDTO c) -> segmentIndex(c.getSegmentCode()))
                        .thenComparing(ConsumerCohortDTO::getTotalGrade)
                        .thenComparing(ConsumerCohortDTO::getUsedRounds, Comparator.reverseOrder())
                        .thenComparing(ConsumerCohortDTO::getCohortId)
        );

        for (ConsumerCohortDTO cohort : orderedCohorts) {
            SegmentStateDTO segmentState = stateBySegment.get(cohort.getSegmentCode());
            ConsumerSegmentRule rule = ruleBySegment.get(cohort.getSegmentCode());
            if (segmentState == null || rule == null) {
                throw new IllegalStateException("消费者购买结算失败：缺少人群配置 " + cohort.getSegmentCode());
            }

            List<Candidate> candidates = buildCandidates(
                    context, cohort, segmentState, rule, brandShares, remainingInventory, starResult
            );

            Map<Long, Integer> purchasedByPhone = allocateCohort(
                    cohort.getPopulation(), candidates, remainingInventory
            );

            int purchasedTotal = 0;
            for (Candidate candidate : candidates) {
                int quantity = purchasedByPhone.getOrDefault(candidate.phoneModel.getId(), 0);
                if (quantity <= 0) {
                    continue;
                }
                purchasedTotal += quantity;
                salesByPlayer.merge(candidate.action.getGamePlayerId(), quantity, Integer::sum);
                addNextCohort(nextCohortMap, cohort.getSegmentCode(), candidate.phoneModel.getId(), quantity, 0);

                ConsumerPurchaseDetail detail = new ConsumerPurchaseDetail();
                detail.setSourceCohortId(cohort.getCohortId());
                detail.setSegmentCode(cohort.getSegmentCode());
                detail.setPhoneModelId(candidate.phoneModel.getId());
                detail.setGamePlayerId(candidate.action.getGamePlayerId());
                detail.setPurchaseQuantity(quantity);
                detail.setPerformanceScore(candidate.performanceScore);
                detail.setAdvertisingScore(candidate.advertisingScore);
                detail.setPriceScore(candidate.priceScore);
                detail.setFinalScore(candidate.finalScore);
                details.add(detail);
            }

            int remainingConsumers = cohort.getPopulation() - purchasedTotal;
            if (remainingConsumers > 0) {
                unservedPopulation += remainingConsumers;
                addNextCohort(
                        nextCohortMap,
                        cohort.getSegmentCode(),
                        cohort.getPhoneModelId(),
                        remainingConsumers,
                        cohort.getUsedRounds() + 1
                );
            }
        }

        List<PlayerSalesItem> playerSalesItems = new ArrayList<>();
        Map<Long, PlayerSalesItem> playerSalesByPlayerId = new LinkedHashMap<>();
        for (RoundAction action : context.getActions()) {
            PlayerCostItem cost = costResult.getByPlayerId(action.getGamePlayerId());
            int sold = salesByPlayer.getOrDefault(action.getGamePlayerId(), 0);
            int unsold = Math.max(0, action.getProductionQuantity() - sold);

            BigDecimal salePrice = money(action.getSalePrice());
            BigDecimal salesRevenue = salePrice.multiply(BigDecimal.valueOf(sold)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal liquidationUnitPrice = cost.getComponentUnitCost()
                    .multiply(LIQUIDATION_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal liquidationRevenue = liquidationUnitPrice
                    .multiply(BigDecimal.valueOf(unsold))
                    .setScale(2, RoundingMode.HALF_UP);

            PlayerSalesItem item = new PlayerSalesItem();
            item.setGamePlayerId(action.getGamePlayerId());
            item.setPhoneModelId(action.getPhoneModelId());
            item.setProductionQuantity(action.getProductionQuantity());
            item.setConsumerSalesQuantity(sold);
            item.setUnsoldQuantity(unsold);
            item.setSalePrice(salePrice);
            item.setConsumerSalesRevenue(salesRevenue);
            item.setComponentUnitCost(cost.getComponentUnitCost());
            item.setLiquidationUnitPrice(liquidationUnitPrice);
            item.setLiquidationRevenue(liquidationRevenue);
            item.setTotalRevenue(salesRevenue.add(liquidationRevenue).setScale(2, RoundingMode.HALF_UP));
            playerSalesItems.add(item);
            playerSalesByPlayerId.put(item.getGamePlayerId(), item);
        }

        ConsumerPurchaseSettlementResult result = new ConsumerPurchaseSettlementResult();
        result.setPlayerSalesItems(playerSalesItems);
        result.setPlayerSalesByPlayerId(playerSalesByPlayerId);
        result.setPurchaseDetails(details);
        result.setNextCohorts(new ArrayList<>(nextCohortMap.values()));
        result.setUnservedPopulation(unservedPopulation);
        return result;
    }

    private List<Candidate> buildCandidates(
            SettlementContext context,
            ConsumerCohortDTO cohort,
            SegmentStateDTO state,
            ConsumerSegmentRule rule,
            Map<String, Map<Long, BigDecimal>> brandShares,
            Map<Long, Integer> remainingInventory,
            StarSettlementResult starResult
    ) {
        List<Candidate> result = new ArrayList<>();
        BigDecimal budget = BigDecimal.valueOf(state.getAverageBudget());
        BigDecimal priceCeiling = budget.multiply(PRICE_CEILING_FACTOR, MC);

        for (RoundAction action : context.getActions()) {
            if (remainingInventory.getOrDefault(action.getGamePlayerId(), 0) <= 0) {
                continue;
            }
            if (action.getSalePrice().compareTo(priceCeiling) > 0) {
                continue;
            }
            PhoneModel model = context.getPhoneModelsById().get(action.getPhoneModelId());
            BigDecimal performanceScore = calculatePerformanceScore(cohort, model, rule);
            if (performanceScore.compareTo(PERFORMANCE_THRESHOLD) < 0) {
                continue;
            }
            BigDecimal advertisingScore = calculateAdvertisingScore(
                    cohort.getSegmentCode(), action, brandShares, starResult
            );
            BigDecimal priceScore = calculatePriceScore(budget, action.getSalePrice());
            BigDecimal finalScore = performanceScore
                    .multiply(advertisingScore, MC)
                    .multiply(priceScore, MC)
                    .setScale(8, RoundingMode.HALF_UP);
            if (finalScore.signum() <= 0) {
                continue;
            }
            result.add(new Candidate(model, action, performanceScore, advertisingScore, priceScore, finalScore));
        }
        result.sort(Comparator
                .comparing((Candidate c) -> c.action.getGamePlayerId())
                .thenComparing(c -> c.phoneModel.getId()));
        return result;
    }

    private BigDecimal calculatePerformanceScore(
            ConsumerCohortDTO cohort,
            PhoneModel model,
            ConsumerSegmentRule rule
    ) {
        BigDecimal score = BigDecimal.valueOf(cohort.getUsedRounds());
        score = score.add(gap(model.getScreenLevel(), cohort.getScreenLevel(), rule.getScreenPreference()));
        score = score.add(gap(model.getProcessorLevel(), cohort.getProcessorLevel(), rule.getProcessorPreference()));
        score = score.add(gap(model.getBodyLevel(), cohort.getBodyLevel(), rule.getBodyPreference()));
        score = score.add(gap(model.getBatteryLevel(), cohort.getBatteryLevel(), rule.getBatteryPreference()));
        score = score.add(gap(model.getStorageLevel(), cohort.getStorageLevel(), rule.getStoragePreference()));
        score = score.add(gap(model.getCameraLevel(), cohort.getCameraLevel(), rule.getCameraPreference()));
        return score.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal gap(Integer next, Integer current, BigDecimal preference) {
        return BigDecimal.valueOf(next - current)
                .multiply(preference, MC)
                .multiply(COMPONENT_GAP_FACTOR, MC);
    }

    private BigDecimal calculateAdvertisingScore(
            String segmentCode,
            RoundAction action,
            Map<String, Map<Long, BigDecimal>> brandShares,
            StarSettlementResult starResult
    ) {
        int adCount = 0;
        if (Boolean.TRUE.equals(action.getFilmAd())) adCount++;
        if (Boolean.TRUE.equals(action.getOnlineAd())) adCount++;
        if (Boolean.TRUE.equals(action.getMagazineAd())) adCount++;
        BigDecimal baseAdBoost = AD_BOOST.multiply(BigDecimal.valueOf(adCount));

        BigDecimal starBoost = BigDecimal.ZERO;
        if (starResult.isHasWinner() && Objects.equals(starResult.getWinnerGamePlayerId(), action.getGamePlayerId())) {
            starBoost = starResult.getBoost();
            if (Objects.equals(starResult.getTargetSegmentCode(), segmentCode)) {
                starBoost = starBoost.multiply(new BigDecimal("2"));
            }
        }

        BigDecimal brandShare = brandShares
                .getOrDefault(segmentCode, Map.of())
                .getOrDefault(action.getGamePlayerId(), BigDecimal.ZERO);

        return BigDecimal.ONE
                .add(baseAdBoost)
                .add(starBoost)
                .multiply(BigDecimal.ONE.add(brandShare.multiply(BRAND_SHARE_FACTOR, MC)), MC)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePriceScore(BigDecimal budget, BigDecimal price) {
        BigDecimal difference = budget.subtract(price);
        BigDecimal score;
        if (difference.signum() >= 0) {
            score = BigDecimal.ONE.add(
                    difference.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("0.025"), MC)
            );
        } else {
            score = BigDecimal.ONE.subtract(
                    difference.abs().divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("0.05"), MC)
            );
        }
        return score.max(MIN_PRICE_SCORE).setScale(8, RoundingMode.HALF_UP);
    }

    private Map<Long, Integer> allocateCohort(
            int population,
            List<Candidate> allCandidates,
            Map<Long, Integer> remainingInventory
    ) {
        Map<Long, Integer> allocatedByPhone = new LinkedHashMap<>();
        int remaining = population;
        List<Candidate> active = allCandidates.stream()
                .filter(c -> remainingInventory.getOrDefault(c.action.getGamePlayerId(), 0) > 0)
                .collect(Collectors.toCollection(ArrayList::new));

        while (remaining > 0 && !active.isEmpty()) {
            BigDecimal totalScore = active.stream()
                    .map(c -> c.finalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalScore.signum() <= 0) break;

            List<Share> shares = new ArrayList<>();
            int floorTotal = 0;
            for (Candidate candidate : active) {
                BigDecimal exact = BigDecimal.valueOf(remaining)
                        .multiply(candidate.finalScore, MC)
                        .divide(totalScore, 16, RoundingMode.HALF_UP);
                int floor = exact.setScale(0, RoundingMode.FLOOR).intValueExact();
                floorTotal += floor;
                shares.add(new Share(candidate, floor, exact.subtract(BigDecimal.valueOf(floor))));
            }
            int leftovers = remaining - floorTotal;
            shares.sort(Comparator
                    .comparing(Share::fraction, Comparator.reverseOrder())
                    .thenComparing(s -> s.candidate.action.getGamePlayerId())
                    .thenComparing(s -> s.candidate.phoneModel.getId()));
            if (leftovers > 0) {
                for (int i = 0; i < leftovers; i++) {
                    shares.get(i % shares.size()).desired++;
                }
            } else if (leftovers < 0) {
                List<Share> reverse = new ArrayList<>(shares);
                Collections.reverse(reverse);
                int toRemove = -leftovers;
                for (int i = 0; i < reverse.size() && toRemove > 0; i++) {
                    Share share = reverse.get(i);
                    if (share.desired > 0) {
                        share.desired--;
                        toRemove--;
                    }
                }
            }

            int allocatedThisPass = 0;
            for (Share share : shares) {
                long playerId = share.candidate.action.getGamePlayerId();
                int stock = remainingInventory.getOrDefault(playerId, 0);
                int actual = Math.min(share.desired, stock);
                if (actual > 0) {
                    remainingInventory.put(playerId, stock - actual);
                    allocatedByPhone.merge(share.candidate.phoneModel.getId(), actual, Integer::sum);
                    allocatedThisPass += actual;
                }
            }
            if (allocatedThisPass <= 0) break;
            remaining -= allocatedThisPass;
            active.removeIf(c -> remainingInventory.getOrDefault(c.action.getGamePlayerId(), 0) <= 0);
        }
        return allocatedByPhone;
    }

    private Map<String, Map<Long, BigDecimal>> buildBrandShareSnapshot(List<ConsumerCohortDTO> cohorts) {
        Map<String, Integer> totalBySegment = new HashMap<>();
        Map<String, Map<Long, Integer>> ownersBySegment = new HashMap<>();
        for (ConsumerCohortDTO cohort : cohorts) {
            totalBySegment.merge(cohort.getSegmentCode(), cohort.getPopulation(), Integer::sum);
            if (cohort.getOwnerGamePlayerId() != null) {
                ownersBySegment.computeIfAbsent(cohort.getSegmentCode(), k -> new HashMap<>())
                        .merge(cohort.getOwnerGamePlayerId(), cohort.getPopulation(), Integer::sum);
            }
        }
        Map<String, Map<Long, BigDecimal>> result = new HashMap<>();
        for (String segment : totalBySegment.keySet()) {
            int total = totalBySegment.get(segment);
            Map<Long, BigDecimal> shares = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : ownersBySegment.getOrDefault(segment, Map.of()).entrySet()) {
                shares.put(entry.getKey(), BigDecimal.valueOf(entry.getValue())
                        .divide(BigDecimal.valueOf(total), 8, RoundingMode.HALF_UP));
            }
            result.put(segment, shares);
        }
        return result;
    }

    private void addNextCohort(
            Map<String, NextConsumerCohortDraft> map,
            String segmentCode,
            Long phoneModelId,
            int population,
            int usedRounds
    ) {
        if (population <= 0) return;
        String key = segmentCode + ":" + phoneModelId + ":" + usedRounds;
        NextConsumerCohortDraft draft = map.get(key);
        if (draft == null) {
            draft = new NextConsumerCohortDraft();
            draft.setSegmentCode(segmentCode);
            draft.setPhoneModelId(phoneModelId);
            draft.setPopulation(population);
            draft.setUsedRounds(usedRounds);
            map.put(key, draft);
        } else {
            draft.setPopulation(draft.getPopulation() + population);
        }
    }

    private int segmentIndex(String segmentCode) {
        int index = SEGMENT_ORDER.indexOf(segmentCode);
        if (index < 0) throw new IllegalArgumentException("未知消费者类型：" + segmentCode);
        return index;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validate(SettlementContext context, PlayerCostSettlementResult costResult, StarSettlementResult starResult) {
        if (context == null || costResult == null || starResult == null) {
            throw new IllegalArgumentException("消费者购买结算参数不能为空");
        }
    }

    private static final class Candidate {
        private final PhoneModel phoneModel;
        private final RoundAction action;
        private final BigDecimal performanceScore;
        private final BigDecimal advertisingScore;
        private final BigDecimal priceScore;
        private final BigDecimal finalScore;

        private Candidate(PhoneModel phoneModel, RoundAction action, BigDecimal performanceScore,
                          BigDecimal advertisingScore, BigDecimal priceScore, BigDecimal finalScore) {
            this.phoneModel = phoneModel;
            this.action = action;
            this.performanceScore = performanceScore;
            this.advertisingScore = advertisingScore;
            this.priceScore = priceScore;
            this.finalScore = finalScore;
        }
    }

    private static final class Share {
        private final Candidate candidate;
        private int desired;
        private final BigDecimal fraction;

        private Share(Candidate candidate, int desired, BigDecimal fraction) {
            this.candidate = candidate;
            this.desired = desired;
            this.fraction = fraction;
        }

        private BigDecimal fraction() {
            return fraction;
        }
    }
}
