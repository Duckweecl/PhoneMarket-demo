package phonemarket.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.entity.RoundAction;
import phonemarket.entity.RoundStar;
import phonemarket.mapper.RoundStarMapper;
import phonemarket.settlement.SettlementContext;
import phonemarket.settlement.StarSettlementResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StarSettlementService {

    private final RoundStarMapper roundStarMapper;

    @Transactional
    public StarSettlementResult settle(
            SettlementContext context
    ) {
        validateContext(context);

        RoundStar star = context.getStar();

        /*
         * 如果已经结算，直接返回已有结果。
         * 保证重复调用不会重新选赢家。
         */
        if (star.getWinningBid() != null) {
            return buildResult(star);
        }

        List<RoundAction> actions =
                context.getActions();

        /*
         * 找到最高报价。
         */
        BigDecimal highestBid = actions.stream()
                .map(RoundAction::getStarBid)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        /*
         * 所有报价均为0：
         * 本回合无人签约。
         */
        if (highestBid.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            int updatedRows =
                    roundStarMapper
                            .settleWithoutWinner(
                                    star.getRoundId()
                            );

            if (updatedRows == 0) {
                return loadExistingResult(
                        star.getRoundId()
                );
            }

            return loadExistingResult(
                    star.getRoundId()
            );
        }

        /*
         * 找出所有并列最高报价玩家。
         */
        List<RoundAction> highestActions =
                actions.stream()
                        .filter(action ->
                                zeroIfNull(
                                        action.getStarBid()
                                ).compareTo(
                                        highestBid
                                ) == 0
                        )
                        .sorted(
                                Comparator.comparing(
                                        RoundAction::getGamePlayerId
                                )
                        )
                        .toList();

        if (highestActions.isEmpty()) {
            throw new IllegalStateException(
                    "明星结算失败：存在最高报价，但找不到对应玩家"
            );
        }

        /*
         * 并列最高报价时使用确定性随机。
         *
         * 先按gamePlayerId排序，再以roundId作为随机种子。
         * 同一回合重复执行时结果保持一致。
         */
        int winnerIndex =
                new Random(
                        star.getRoundId()
                ).nextInt(
                        highestActions.size()
                );

        RoundAction winnerAction =
                highestActions.get(
                        winnerIndex
                );

        int updatedRows =
                roundStarMapper.settleWithWinner(
                        star.getRoundId(),
                        winnerAction.getGamePlayerId(),
                        highestBid
                );

        /*
         * 返回0表示可能已经被其他线程结算。
         * 重新读取数据库中的真实结果。
         */
        if (updatedRows == 0) {
            return loadExistingResult(
                    star.getRoundId()
            );
        }

        return loadExistingResult(
                star.getRoundId()
        );
    }

    private void validateContext(
            SettlementContext context
    ) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "明星结算失败：SettlementContext不能为空"
            );
        }

        if (context.getRound() == null) {
            throw new IllegalStateException(
                    "明星结算失败：当前回合不存在"
            );
        }

        if (!"PROCESSING".equals(
                context.getRound().getStatus()
        )) {
            throw new IllegalStateException(
                    "明星结算失败：回合状态不是PROCESSING"
            );
        }

        if (context.getStar() == null) {
            throw new IllegalStateException(
                    "明星结算失败：明星数据不存在"
            );
        }

        if (context.getActions() == null ||
                context.getActions().isEmpty()) {
            throw new IllegalStateException(
                    "明星结算失败：玩家提交不存在"
            );
        }

        for (RoundAction action :
                context.getActions()) {

            if (action.getGamePlayerId() == null) {
                throw new IllegalStateException(
                        "明星结算失败：存在没有玩家ID的提交"
                );
            }

            if (action.getStarBid() == null) {
                throw new IllegalStateException(
                        "明星结算失败：玩家明星报价为空，gamePlayerId="
                                + action.getGamePlayerId()
                );
            }

            if (action.getStarBid().compareTo(
                    BigDecimal.ZERO
            ) < 0) {
                throw new IllegalStateException(
                        "明星结算失败：玩家明星报价小于0，gamePlayerId="
                                + action.getGamePlayerId()
                );
            }
        }
    }

    private StarSettlementResult loadExistingResult(
            long roundId
    ) {
        RoundStar settledStar =
                roundStarMapper.findByRoundId(
                        roundId
                );

        if (settledStar == null) {
            throw new IllegalStateException(
                    "明星结算失败：结算后无法读取明星数据"
            );
        }

        if (settledStar.getWinningBid() == null) {
            throw new IllegalStateException(
                    "明星结算失败：明星结果没有成功保存"
            );
        }

        return buildResult(settledStar);
    }

    private StarSettlementResult buildResult(
            RoundStar star
    ) {
        StarSettlementResult result =
                new StarSettlementResult();

        result.setWinnerGamePlayerId(
                star.getWinnerGamePlayerId()
        );

        result.setSigningFee(
                zeroIfNull(
                        star.getWinningBid()
                )
        );

        result.setTargetSegmentCode(
                star.getTargetSegmentCode()
        );

        result.setBoost(
                star.getBoost()
        );

        result.setHasWinner(
                star.getWinnerGamePlayerId() != null
                        && zeroIfNull(
                        star.getWinningBid()
                ).compareTo(
                        BigDecimal.ZERO
                ) > 0
        );

        return result;
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}