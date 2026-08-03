package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.RoundActionRequest;
import phonemarket.dto.RoundActionResponse;
import phonemarket.entity.*;
import phonemarket.mapper.*;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RoundSubmissionService {
    private static final BigDecimal FILM_AD_COST_PER_PERSON = new BigDecimal("100");
    private static final BigDecimal ONLINE_AD_COST_PER_PERSON = new BigDecimal("25");
    private static final BigDecimal MAGAZINE_AD_COST_PER_PERSON = new BigDecimal("10");
    private static final BigDecimal LOW_ASSEMBLY_COST = new BigDecimal("500");
    private static final BigDecimal MEDIUM_ASSEMBLY_COST = new BigDecimal("1000");
    private static final BigDecimal HIGH_ASSEMBLY_COST = new BigDecimal("2000");

    private final GameMapper gameMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundMapper roundMapper;
    private final PhoneModelMapper phoneModelMapper;
    private final RoundActionMapper roundActionMapper;
    private final RoundComponentMarketMapper componentMarketMapper;
    private final RoundSegmentStateMapper segmentStateMapper;

    @Transactional
    public RoundActionResponse submit(long userId, long gameId, RoundActionRequest request) {
        validateRequest(request);
        Game game = gameMapper.findById(gameId);
        if (game == null) throw status(HttpStatus.NOT_FOUND, "游戏不存在");
        if (!"RUNNING".equals(game.getStatus())) throw status(HttpStatus.CONFLICT, "游戏当前不是运行状态");

        GamePlayer player = gamePlayerMapper.findByGameAndUser(gameId, userId);
        if (player == null) throw status(HttpStatus.FORBIDDEN, "你不属于该游戏");
        if (!"ACTIVE".equals(player.getStatus())) throw status(HttpStatus.CONFLICT, "当前玩家不是有效状态");

        Round round = roundMapper.findByGameAndRoundNo(gameId, game.getCurrentRound());
        if (round == null || !"COLLECTING".equals(round.getStatus())) {
            throw status(HttpStatus.CONFLICT, "当前回合不允许提交");
        }
        if (roundActionMapper.countByRoundAndPlayer(round.getId(), player.getId()) > 0) {
            throw status(HttpStatus.CONFLICT, "本回合已经提交，不能重复提交");
        }

        BigDecimal plannedSpending = calculatePlannedSpending(round.getId(), request);
        BigDecimal cash = zero(player.getCash());
        BigDecimal availableCredit = zero(player.getDebtLimit()).subtract(zero(player.getDebt())).max(BigDecimal.ZERO);
        BigDecimal maximumSpending = cash.add(availableCredit);
        if (plannedSpending.compareTo(maximumSpending) > 0) {
            throw status(HttpStatus.CONFLICT,
                    "计划支出超过最大支出范围。计划支出：" + plannedSpending + "，最大支出范围：" + maximumSpending);
        }

        PhoneModel model = buildPhoneModel(round.getId(), player.getId(), request);
        RoundAction action;
        try {
            phoneModelMapper.insertPlayerModel(model);
            action = buildRoundAction(round.getId(), player.getId(), model.getId(), request);
            roundActionMapper.insert(action);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "本回合已经提交，不能重复提交", e);
        }

        if (roundMapper.increaseSubmittedCount(round.getId()) != 1) {
            throw status(HttpStatus.CONFLICT, "提交人数更新失败，当前回合可能已关闭");
        }
        Round updated = roundMapper.findById(round.getId());
        boolean allSubmitted = updated.getSubmittedCount() >= updated.getExpectedPlayerCount();
        if (allSubmitted && roundMapper.claimSettlement(updated.getId()) == 1) {
            updated = roundMapper.findById(updated.getId());
        }

        RoundActionResponse response = new RoundActionResponse();
        response.setRoundId(updated.getId());
        response.setRoundNo(updated.getRoundNo());
        response.setPhoneModelId(model.getId());
        response.setActionId(action.getId());
        response.setModelName(model.getModelName());
        response.setSubmittedCount(updated.getSubmittedCount());
        response.setExpectedPlayerCount(updated.getExpectedPlayerCount());
        response.setAllSubmitted(allSubmitted);
        response.setRoundStatus(updated.getStatus());
        response.setMessage("PROCESSING".equals(updated.getStatus())
                ? "提交成功，所有玩家均已提交，正在结算"
                : "提交成功，等待其他玩家");
        return response;
    }

    private BigDecimal calculatePlannedSpending(long roundId, RoundActionRequest request) {
        BigDecimal componentUnit = BigDecimal.ZERO;
        componentUnit = componentUnit.add(price(roundId, "SCREEN", request.getScreenLevel()));
        componentUnit = componentUnit.add(price(roundId, "PROCESSOR", request.getProcessorLevel()));
        componentUnit = componentUnit.add(price(roundId, "BODY", request.getBodyLevel()));
        componentUnit = componentUnit.add(price(roundId, "BATTERY", request.getBatteryLevel()));
        componentUnit = componentUnit.add(price(roundId, "STORAGE", request.getStorageLevel()));
        componentUnit = componentUnit.add(price(roundId, "CAMERA", request.getCameraLevel()));
        BigDecimal production = componentUnit.add(assemblyCost(request))
                .multiply(BigDecimal.valueOf(request.getProductionQuantity()));
        int population = segmentStateMapper.sumPopulationByRoundId(roundId);
        if (population <= 0) throw new IllegalStateException("当前回合消费者人口数据不存在");
        BigDecimal ads = BigDecimal.ZERO;
        BigDecimal pop = BigDecimal.valueOf(population);
        if (Boolean.TRUE.equals(request.getFilmAd())) ads = ads.add(pop.multiply(FILM_AD_COST_PER_PERSON));
        if (Boolean.TRUE.equals(request.getOnlineAd())) ads = ads.add(pop.multiply(ONLINE_AD_COST_PER_PERSON));
        if (Boolean.TRUE.equals(request.getMagazineAd())) ads = ads.add(pop.multiply(MAGAZINE_AD_COST_PER_PERSON));
        return production.add(ads).add(BigDecimal.valueOf(request.getStarBid()));
    }

    private BigDecimal price(long roundId, String type, int level) {
        BigDecimal price = componentMarketMapper.findActualUnitPrice(roundId, type, level);
        if (price == null) throw new IllegalStateException("找不到零部件市场价格：" + type + ":" + level);
        return price;
    }

    private BigDecimal assemblyCost(RoundActionRequest request) {
        int[] levels = {request.getScreenLevel(), request.getProcessorLevel(), request.getBodyLevel(),
                request.getBatteryLevel(), request.getStorageLevel(), request.getCameraLevel()};
        int level3 = 0;
        int level2Plus = 0;
        for (int level : levels) {
            if (level == 3) level3++;
            if (level >= 2) level2Plus++;
        }
        if (level3 >= 2) return HIGH_ASSEMBLY_COST;
        if (level2Plus >= 2) return MEDIUM_ASSEMBLY_COST;
        return LOW_ASSEMBLY_COST;
    }

    private PhoneModel buildPhoneModel(long roundId, long playerId, RoundActionRequest request) {
        PhoneModel model = new PhoneModel();
        model.setRoundId(roundId);
        model.setGamePlayerId(playerId);
        model.setModelName(request.getModelName().trim());
        model.setModelType("PLAYER");
        model.setScreenLevel(request.getScreenLevel());
        model.setProcessorLevel(request.getProcessorLevel());
        model.setBodyLevel(request.getBodyLevel());
        model.setBatteryLevel(request.getBatteryLevel());
        model.setStorageLevel(request.getStorageLevel());
        model.setCameraLevel(request.getCameraLevel());
        return model;
    }

    private RoundAction buildRoundAction(long roundId, long playerId, long modelId, RoundActionRequest request) {
        RoundAction action = new RoundAction();
        action.setRoundId(roundId);
        action.setGamePlayerId(playerId);
        action.setPhoneModelId(modelId);
        action.setProductionQuantity(request.getProductionQuantity());
        action.setSalePrice(BigDecimal.valueOf(request.getSalePrice()));
        action.setFilmAd(Boolean.TRUE.equals(request.getFilmAd()));
        action.setOnlineAd(Boolean.TRUE.equals(request.getOnlineAd()));
        action.setMagazineAd(Boolean.TRUE.equals(request.getMagazineAd()));
        action.setStarBid(BigDecimal.valueOf(request.getStarBid()));
        return action;
    }

    private void validateRequest(RoundActionRequest request) {
        if (request == null) throw status(HttpStatus.BAD_REQUEST, "提交内容不能为空");
        if (request.getModelName() == null || request.getModelName().trim().isEmpty()) {
            throw status(HttpStatus.BAD_REQUEST, "手机型号名称不能为空");
        }
        if (request.getModelName().trim().length() > 50) throw status(HttpStatus.BAD_REQUEST, "手机型号名称不能超过50个字符");
        validateLevel("屏幕", request.getScreenLevel());
        validateLevel("处理器", request.getProcessorLevel());
        validateLevel("机身", request.getBodyLevel());
        validateLevel("电池", request.getBatteryLevel());
        validateLevel("存储", request.getStorageLevel());
        validateLevel("相机", request.getCameraLevel());
        if (request.getProductionQuantity() == null || request.getProductionQuantity() < 0) {
            throw status(HttpStatus.BAD_REQUEST, "生产数量不能小于0");
        }
        if (request.getSalePrice() == null || request.getSalePrice() <= 0) {
            throw status(HttpStatus.BAD_REQUEST, "手机售价必须大于0");
        }
        if (request.getStarBid() == null || request.getStarBid() < 0) {
            throw status(HttpStatus.BAD_REQUEST, "明星报价不能小于0");
        }
    }

    private void validateLevel(String name, Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw status(HttpStatus.BAD_REQUEST, name + "配置不正确");
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ResponseStatusException status(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
