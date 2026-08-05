package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phonemarket.dto.RoundActionRequest;
import phonemarket.dto.RoundActionResponse;
import phonemarket.mapper.GameMapper;

@Service
@RequiredArgsConstructor
public class PlayService {

    private final RoundSubmissionService roundSubmissionService;
    private final RoundSettlementTriggerService roundSettlementTriggerService;
    private final GameMapper gameMapper;

    /**
     * 只有成功保存的提交才会刷新游戏的最后活动时间。
     */
    public RoundActionResponse submitAction(
            long userId,
            long gameId,
            RoundActionRequest request
    ) {
        RoundActionResponse response =
                roundSubmissionService.submit(userId, gameId, request);

        gameMapper.touchActivity(gameId);

        if ("PROCESSING".equals(response.getRoundStatus())) {
            roundSettlementTriggerService.trigger(
                    gameId,
                    response.getRoundId()
            );
            response.setMessage(
                    "提交成功，所有玩家均已提交，市场正在结算"
            );
        }

        return response;
    }
}
