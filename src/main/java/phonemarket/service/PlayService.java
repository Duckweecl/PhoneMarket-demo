package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phonemarket.dto.RoundActionRequest;
import phonemarket.dto.RoundActionResponse;

@Service
@RequiredArgsConstructor
public class PlayService {
    private final RoundSubmissionService roundSubmissionService;
    private final RoundSettlementTriggerService roundSettlementTriggerService;

    public RoundActionResponse submitAction(long userId, long gameId, RoundActionRequest request) {
        RoundActionResponse response = roundSubmissionService.submit(userId, gameId, request);
        if ("PROCESSING".equals(response.getRoundStatus())) {
            roundSettlementTriggerService.trigger(gameId, response.getRoundId());
            response.setMessage("提交成功，所有玩家均已提交，市场正在结算");
        }
        return response;
    }
}
