package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phonemarket.entity.Round;
import phonemarket.entity.RoundPlayerResult;
import phonemarket.mapper.*;
import phonemarket.settlement.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoundSettlementService {

    private final RoundMapper roundMapper;
    private final GameMapper gameMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundPlayerResultMapper roundPlayerResultMapper;
    private final SettlementContextService settlementContextService;
    private final StarSettlementService starSettlementService;
    private final ComponentMarketSettlementService componentMarketSettlementService;
    private final PlayerCostSettlementService playerCostSettlementService;
    private final ConsumerPurchaseSettlementService consumerPurchaseSettlementService;
    private final PlayerFinancialSettlementService playerFinancialSettlementService;
    private final RoundInitializationService roundInitializationService;

    @Transactional
    public void settle(long gameId, long roundId) {
        Round lockedRound = roundMapper.findByIdForUpdate(roundId);
        if (lockedRound == null || !lockedRound.getGameId().equals(gameId)) {
            throw new IllegalStateException("回合结算失败：回合不存在");
        }
        if ("FINISHED".equals(lockedRound.getStatus())) {
            return;
        }
        if (!"PROCESSING".equals(lockedRound.getStatus())) {
            throw new IllegalStateException("回合结算失败：回合状态不是PROCESSING");
        }
        if (roundPlayerResultMapper.countByRoundId(roundId) > 0) {
            throw new IllegalStateException("回合结算失败：已存在玩家结算结果");
        }

        SettlementContext context = settlementContextService.build(gameId, roundId);
        StarSettlementResult starResult = starSettlementService.settle(context);
        ComponentMarketSettlementResult componentResult = componentMarketSettlementService.settle(context);
        PlayerCostSettlementResult costResult = playerCostSettlementService.calculate(context, componentResult, starResult);
        ConsumerPurchaseSettlementResult purchaseResult = consumerPurchaseSettlementService.settle(
                context, costResult, starResult
        );
        FinancialSettlementResult financialResult = playerFinancialSettlementService.settle(
                context, costResult, purchaseResult, starResult
        );

        List<RoundPlayerResult> rows = financialResult.getItems().stream()
                .map(PlayerFinancialItem::getRoundPlayerResult)
                .toList();
        if (roundPlayerResultMapper.batchInsert(rows) != rows.size()) {
            throw new IllegalStateException("回合结算失败：玩家结果保存不完整");
        }

        for (PlayerFinancialItem item : financialResult.getItems()) {
            int updated = gamePlayerMapper.updateFinancialState(
                    item.getGamePlayerId(),
                    gameId,
                    item.getEndingCash(),
                    item.getEndingDebt(),
                    item.getEndingTotalSales(),
                    item.getEndingCumulativeSalesProfit(),
                    item.getEndingTotalSettlementProfit()
            );
            if (updated != 1) {
                throw new IllegalStateException("回合结算失败：玩家资金更新失败，gamePlayerId=" + item.getGamePlayerId());
            }
        }

        boolean finalRound = lockedRound.getRoundNo() >= context.getGame().getMaxRound();
        if (!finalRound) {
            Round nextRound = roundInitializationService.initializeNextRound(
                    gameId,
                    lockedRound,
                    lockedRound.getExpectedPlayerCount(),
                    context.getSegmentStates(),
                    purchaseResult.getNextCohorts(),
                    componentResult
            );
            if (gameMapper.setCurrentRound(gameId, nextRound.getRoundNo()) != 1) {
                throw new IllegalStateException("回合结算失败：无法切换到下一回合");
            }
        }

        if (roundMapper.finishRound(roundId) != 1) {
            throw new IllegalStateException("回合结算失败：无法结束当前回合");
        }
        if (finalRound && gameMapper.finishRunningGame(gameId) != 1) {
            throw new IllegalStateException("回合结算失败：无法结束游戏");
        }
    }
}
