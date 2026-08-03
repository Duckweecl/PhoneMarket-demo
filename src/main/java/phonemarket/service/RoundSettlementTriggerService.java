package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class RoundSettlementTriggerService {

    private static final Logger LOGGER = Logger.getLogger(RoundSettlementTriggerService.class.getName());

    private final RoundSettlementService roundSettlementService;
    private final Set<Long> inFlightRoundIds = ConcurrentHashMap.newKeySet();

    @Async
    public void trigger(long gameId, long roundId) {
        if (!inFlightRoundIds.add(roundId)) {
            return;
        }
        try {
            roundSettlementService.settle(gameId, roundId);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "回合异步结算失败，gameId=" + gameId + ", roundId=" + roundId, exception);
        } finally {
            inFlightRoundIds.remove(roundId);
        }
    }
}
