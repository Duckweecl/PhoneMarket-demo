package phonemarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import phonemarket.dto.*;
import phonemarket.entity.*;
import phonemarket.mapper.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameRoundOverviewService {

    private final GameMapper gameMapper;
    private final GamePlayerMapper gamePlayerMapper;
    private final RoundMapper roundMapper;
    private final RoundActionMapper roundActionMapper;
    private final RoundSegmentStateMapper segmentStateMapper;
    private final RoundComponentMarketMapper componentMarketMapper;
    private final RoundConsumerCohortMapper consumerCohortMapper;
    private final RoundStarMapper roundStarMapper;
    private final RoundPlayerResultMapper roundPlayerResultMapper;
    private final PhoneModelMapper phoneModelMapper;

    public GameRoundOverviewResponse getOverview(long userId, long gameId) {
        Game game = requireGame(gameId);
        GamePlayer currentPlayer = requirePlayer(gameId, userId);
        Round currentRound = requireRound(gameId, game.getCurrentRound());

        List<PlayerOverviewDTO> players = gamePlayerMapper.findOverviewPlayers(gameId, userId);
        applyCompetitionRanks(players);
        Map<Long, Integer> ranks = players.stream().collect(Collectors.toMap(
                PlayerOverviewDTO::getGamePlayerId, PlayerOverviewDTO::getRank
        ));

        GameRoundOverviewResponse response = new GameRoundOverviewResponse();
        response.setGameId(game.getId());
        response.setGameStatus(game.getStatus());
        response.setCurrentRoundNo(currentRound.getRoundNo());
        response.setMaxRound(game.getMaxRound());
        response.setSubmittedCount(currentRound.getSubmittedCount());
        response.setExpectedPlayerCount(currentRound.getExpectedPlayerCount());
        response.setCurrentPlayerSubmitted(
                roundActionMapper.countByRoundAndPlayer(currentRound.getId(), currentPlayer.getId()) > 0
        );
        response.setGameFinished("FINISHED".equals(game.getStatus()));
        response.setCurrentPlayer(buildCurrentPlayer(currentPlayer, ranks.get(currentPlayer.getId())));
        response.setPlayers(players);
        response.setCurrentRound(buildCurrentRound(currentRound, gameId));

        int resultRoundNo;
        if ("FINISHED".equals(game.getStatus())) {
            resultRoundNo = currentRound.getRoundNo();
        } else {
            resultRoundNo = currentRound.getRoundNo() - 1;
        }
        response.setPreviousRound(resultRoundNo <= 0
                ? createRoundZero()
                : buildPreviousRound(gameId, resultRoundNo, currentPlayer.getId()));
        return response;
    }

    public GameRoundStatusDTO getStatus(long userId, long gameId) {
        Game game = requireGame(gameId);
        GamePlayer player = requirePlayer(gameId, userId);
        Round round = requireRound(gameId, game.getCurrentRound());
        GameRoundStatusDTO dto = new GameRoundStatusDTO();
        dto.setGameId(gameId);
        dto.setGameStatus(game.getStatus());
        dto.setCurrentRoundId(round.getId());
        dto.setCurrentRoundNo(round.getRoundNo());
        dto.setRoundStatus(round.getStatus());
        dto.setSubmittedCount(round.getSubmittedCount());
        dto.setExpectedPlayerCount(round.getExpectedPlayerCount());
        dto.setCurrentPlayerSubmitted(
                roundActionMapper.countByRoundAndPlayer(round.getId(), player.getId()) > 0
        );
        return dto;
    }

    private CurrentPlayerDTO buildCurrentPlayer(GamePlayer player, Integer rank) {
        CurrentPlayerDTO dto = new CurrentPlayerDTO();
        dto.setGamePlayerId(player.getId());
        dto.setUsername(gamePlayerMapper.findUsernameByUserId(player.getUserId()));
        dto.setStatus(player.getStatus());
        dto.setRank(rank);
        dto.setCash(money(player.getCash()));
        dto.setDebt(money(player.getDebt()));
        dto.setDebtLimit(money(player.getDebtLimit()));
        dto.setAvailableCredit(money(player.getDebtLimit()).subtract(money(player.getDebt())).max(BigDecimal.ZERO));
        dto.setCumulativeSalesProfit(money(player.getCumulativeSalesProfit()));
        dto.setTotalSettlementProfit(money(player.getTotalSettlementProfit()));
        return dto;
    }

    private CurrentRoundDTO buildCurrentRound(Round currentRound, long gameId) {
        CurrentRoundDTO dto = new CurrentRoundDTO();
        dto.setRoundId(currentRound.getId());
        dto.setRoundNo(currentRound.getRoundNo());
        dto.setStatus(currentRound.getStatus());
        dto.setEconomyFactor(currentRound.getEconomyFactor());
        dto.setSegments(segmentStateMapper.findSegmentDTOsByRoundId(currentRound.getId()));

        List<ConsumerCohortDTO> cohorts = consumerCohortMapper.findCohortDTOsByRoundId(currentRound.getId());
        dto.setConsumerCohorts(cohorts);
        dto.setSegmentHoldings(buildSegmentHoldings(cohorts));

        List<ComponentMarketDTO> currentMarkets = componentMarketMapper.findComponentDTOsByRoundId(currentRound.getId());
        if (currentRound.getRoundNo() > 1) {
            Round previous = roundMapper.findByGameAndRoundNo(gameId, currentRound.getRoundNo() - 1);
            if (previous != null) {
                Map<String, ComponentMarketDTO> previousMap = componentMarketMapper
                        .findComponentDTOsByRoundId(previous.getId()).stream()
                        .collect(Collectors.toMap(this::marketKey, Function.identity()));
                for (ComponentMarketDTO market : currentMarkets) {
                    ComponentMarketDTO old = previousMap.get(marketKey(market));
                    if (old != null) {
                        market.setPreviousDemandQuantity(old.getDemandQuantity());
                        market.setPreviousSupplyQuantity(old.getSupplyQuantity());
                        market.setSupplyChange(market.getSupplyQuantity() - old.getSupplyQuantity());
                    }
                }
            }
        }
        dto.setComponentMarkets(currentMarkets);
        dto.setStar(buildStarPreview(currentRound.getId()));
        return dto;
    }

    private StarDTO buildStarPreview(long roundId) {
        RoundStar star = roundStarMapper.findByRoundId(roundId);
        if (star == null) return null;
        StarDTO dto = new StarDTO();
        dto.setTargetSegmentCode(star.getTargetSegmentCode());
        boolean settled = star.getWinningBid() != null;
        dto.setSettled(settled);
        if (settled) {
            dto.setBoost(star.getBoost());
            dto.setTargetSegmentBoost(star.getBoost().multiply(new BigDecimal("2")));
            dto.setWinnerGamePlayerId(star.getWinnerGamePlayerId());
            dto.setWinningBid(star.getWinningBid());
            if (star.getWinnerGamePlayerId() != null) {
                GamePlayer winner = gamePlayerMapper.findById(star.getWinnerGamePlayerId());
                if (winner != null) {
                    dto.setWinnerCompanyName(gamePlayerMapper.findUsernameByUserId(winner.getUserId()));
                }
            }
        }
        return dto;
    }

    private PreviousRoundDTO buildPreviousRound(long gameId, int roundNo, long currentGamePlayerId) {
        Round round = roundMapper.findByGameAndRoundNo(gameId, roundNo);
        if (round == null || !"FINISHED".equals(round.getStatus())) {
            return createRoundZero();
        }
        PreviousRoundDTO dto = new PreviousRoundDTO();
        dto.setRoundId(round.getId());
        dto.setRoundNo(round.getRoundNo());
        dto.setEconomyFactor(round.getEconomyFactor());
        dto.setComponentMarkets(componentMarketMapper.findComponentDTOsByRoundId(round.getId()));
        dto.setStarResult(buildStarResult(round.getId()));

        List<RoundPlayerResult> rows = roundPlayerResultMapper.findByRoundId(round.getId());
        List<PlayerRoundResultDTO> publicResults = new ArrayList<>();
        for (RoundPlayerResult row : rows) {
            PlayerRoundResultDTO result = new PlayerRoundResultDTO();
            result.setGamePlayerId(row.getGamePlayerId());
            GamePlayer player = gamePlayerMapper.findById(row.getGamePlayerId());
            result.setCompanyName(player == null ? "未知玩家" : gamePlayerMapper.findUsernameByUserId(player.getUserId()));
            result.setPhoneModel(toPhoneModelDTO(phoneModelMapper.findById(row.getPhoneModelId())));
            result.setProductionQuantity(row.getProductionQuantity());
            result.setConsumerSalesQuantity(row.getConsumerSalesQuantity());
            result.setUnsoldQuantity(row.getUnsoldQuantity());
            result.setSalePrice(row.getSalePrice());
            result.setFilmAd(row.getFilmAd());
            result.setOnlineAd(row.getOnlineAd());
            result.setMagazineAd(row.getMagazineAd());
            result.setStarBid(row.getStarBid());
            result.setWonStar(row.getWonStar());
            result.setSalesProfit(row.getSalesProfit());
            publicResults.add(result);
        }
        dto.setPlayerResults(publicResults);

        RoundPlayerResult own = roundPlayerResultMapper.findByRoundAndPlayer(round.getId(), currentGamePlayerId);
        if (own != null) {
            dto.setCurrentPlayerFinancialResult(toFinancialDTO(round, own));
        }
        return dto;
    }

    private StarResultDTO buildStarResult(long roundId) {
        RoundStar star = roundStarMapper.findByRoundId(roundId);
        if (star == null || star.getWinningBid() == null) return null;
        StarResultDTO dto = new StarResultDTO();
        dto.setTargetSegmentCode(star.getTargetSegmentCode());
        dto.setBoost(star.getBoost());
        dto.setTargetSegmentBoost(star.getBoost().multiply(new BigDecimal("2")));
        dto.setWinnerGamePlayerId(star.getWinnerGamePlayerId());
        dto.setWinningBid(star.getWinningBid());
        dto.setBids(roundActionMapper.findStarBidsByRoundId(roundId));
        if (star.getWinnerGamePlayerId() != null) {
            GamePlayer winner = gamePlayerMapper.findById(star.getWinnerGamePlayerId());
            if (winner != null) dto.setWinnerCompanyName(gamePlayerMapper.findUsernameByUserId(winner.getUserId()));
        }
        return dto;
    }

    private PlayerFinancialResultDTO toFinancialDTO(Round round, RoundPlayerResult row) {
        PlayerFinancialResultDTO dto = new PlayerFinancialResultDTO();
        dto.setRoundId(round.getId());
        dto.setRoundNo(round.getRoundNo());
        dto.setGamePlayerId(row.getGamePlayerId());
        dto.setBeginningCash(row.getBeginningCash());
        dto.setBeginningDebt(row.getBeginningDebt());
        dto.setBeginningAvailableCredit(row.getBeginningAvailableCredit());
        dto.setProductionQuantity(row.getProductionQuantity());
        dto.setSalePrice(row.getSalePrice());
        dto.setComponentUnitCost(row.getComponentUnitCost());
        dto.setComponentCost(row.getComponentCost());
        dto.setAssemblyUnitCost(row.getAssemblyUnitCost());
        dto.setAssemblyCost(row.getAssemblyCost());
        dto.setProductionCost(row.getProductionCost());
        dto.setFilmAdvertisingCost(row.getFilmAdvertisingCost());
        dto.setOnlineAdvertisingCost(row.getOnlineAdvertisingCost());
        dto.setMagazineAdvertisingCost(row.getMagazineAdvertisingCost());
        dto.setAdvertisingCost(row.getAdvertisingCost());
        dto.setStarBid(row.getStarBid());
        dto.setWonStar(row.getWonStar());
        dto.setStarCost(row.getStarCost());
        dto.setTotalOperatingCost(row.getProductionCost().add(row.getAdvertisingCost()).add(row.getStarCost()));
        dto.setConsumerSalesQuantity(row.getConsumerSalesQuantity());
        dto.setUnsoldQuantity(row.getUnsoldQuantity());
        dto.setConsumerSalesRevenue(row.getConsumerSalesRevenue());
        dto.setLiquidationUnitPrice(row.getLiquidationUnitPrice());
        dto.setLiquidationRevenue(row.getLiquidationRevenue());
        dto.setTotalRevenue(row.getTotalRevenue());
        dto.setNewNormalLoan(row.getNewNormalLoan());
        dto.setNormalLoanPrincipal(row.getNormalLoanPrincipal());
        dto.setNormalLoanInterest(row.getNormalLoanInterest());
        dto.setPaydayPrincipal(row.getPaydayPrincipal());
        dto.setPaydayInterest(row.getPaydayInterest());
        dto.setTotalRepaymentDue(row.getTotalRepaymentDue());
        dto.setActualRepayment(row.getActualRepayment());
        dto.setEndingCash(row.getEndingCash());
        dto.setEndingDebt(row.getEndingDebt());
        dto.setEndingAvailableCredit(row.getEndingAvailableCredit());
        dto.setRoundCashResult(row.getRoundCashResult());
        dto.setSalesProfit(row.getSalesProfit());
        dto.setRoundSettlementProfit(row.getRoundSettlementProfit());
        dto.setEndingCumulativeSalesProfit(row.getEndingCumulativeSalesProfit());
        dto.setEndingTotalSettlementProfit(row.getEndingTotalSettlementProfit());
        return dto;
    }

    private PhoneModelDTO toPhoneModelDTO(PhoneModel model) {
        if (model == null) return null;
        PhoneModelDTO dto = new PhoneModelDTO();
        dto.setPhoneModelId(model.getId());
        dto.setModelName(model.getModelName());
        dto.setScreenLevel(model.getScreenLevel());
        dto.setProcessorLevel(model.getProcessorLevel());
        dto.setBodyLevel(model.getBodyLevel());
        dto.setBatteryLevel(model.getBatteryLevel());
        dto.setStorageLevel(model.getStorageLevel());
        dto.setCameraLevel(model.getCameraLevel());
        dto.setTotalGrade(model.getTotalGrade());
        return dto;
    }

    private List<SegmentHoldingDTO> buildSegmentHoldings(List<ConsumerCohortDTO> cohorts) {
        Map<String, List<ConsumerCohortDTO>> bySegment = cohorts.stream()
                .collect(Collectors.groupingBy(ConsumerCohortDTO::getSegmentCode, LinkedHashMap::new, Collectors.toList()));
        List<SegmentHoldingDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<ConsumerCohortDTO>> segmentEntry : bySegment.entrySet()) {
            int total = segmentEntry.getValue().stream().mapToInt(ConsumerCohortDTO::getPopulation).sum();
            Map<String, List<ConsumerCohortDTO>> byCompany = segmentEntry.getValue().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getOwnerGamePlayerId() == null ? "SYSTEM" : "PLAYER:" + c.getOwnerGamePlayerId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            List<CompanyHoldingDTO> companies = new ArrayList<>();
            for (Map.Entry<String, List<ConsumerCohortDTO>> companyEntry : byCompany.entrySet()) {
                List<ConsumerCohortDTO> companyCohorts = companyEntry.getValue();
                int companyPopulation = companyCohorts.stream().mapToInt(ConsumerCohortDTO::getPopulation).sum();
                CompanyHoldingDTO company = new CompanyHoldingDTO();
                company.setGamePlayerId(companyCohorts.get(0).getOwnerGamePlayerId());
                company.setCompanyName(company.getGamePlayerId() == null
                        ? "系统品牌"
                        : companyCohorts.get(0).getOwnerCompanyName());
                company.setHoldingPopulation(companyPopulation);
                company.setHoldingRate(rate(companyPopulation, total));

                Map<Long, List<ConsumerCohortDTO>> byModel = companyCohorts.stream()
                        .collect(Collectors.groupingBy(ConsumerCohortDTO::getPhoneModelId, LinkedHashMap::new, Collectors.toList()));
                List<ModelHoldingDTO> models = new ArrayList<>();
                for (List<ConsumerCohortDTO> modelCohorts : byModel.values()) {
                    int modelPopulation = modelCohorts.stream().mapToInt(ConsumerCohortDTO::getPopulation).sum();
                    ModelHoldingDTO model = new ModelHoldingDTO();
                    model.setPhoneModelId(modelCohorts.get(0).getPhoneModelId());
                    model.setModelName(modelCohorts.get(0).getPhoneModelName());
                    model.setPopulation(modelPopulation);
                    model.setHoldingRate(rate(modelPopulation, total));
                    models.add(model);
                }
                company.setModels(models);
                companies.add(company);
            }
            SegmentHoldingDTO segment = new SegmentHoldingDTO();
            segment.setSegmentCode(segmentEntry.getKey());
            segment.setTotalPopulation(total);
            segment.setCompanyHoldings(companies);
            result.add(segment);
        }
        return result;
    }

    private void applyCompetitionRanks(List<PlayerOverviewDTO> players) {
        List<PlayerOverviewDTO> sorted = new ArrayList<>(players);
        sorted.sort(Comparator
                .comparing((PlayerOverviewDTO p) -> money(p.getTotalSettlementProfit()), Comparator.reverseOrder())
                .thenComparing(PlayerOverviewDTO::getSeatNo));
        BigDecimal previous = null;
        int rank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            BigDecimal value = money(sorted.get(i).getTotalSettlementProfit());
            if (previous == null || value.compareTo(previous) != 0) rank = i + 1;
            sorted.get(i).setRank(rank);
            previous = value;
        }
        players.sort(Comparator.comparing(PlayerOverviewDTO::getRank).thenComparing(PlayerOverviewDTO::getSeatNo));
    }

    private BigDecimal rate(int part, int total) {
        return total <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private String marketKey(ComponentMarketDTO dto) {
        return dto.getComponentType() + ":" + dto.getComponentLevel();
    }

    private PreviousRoundDTO createRoundZero() {
        PreviousRoundDTO dto = new PreviousRoundDTO();
        dto.setRoundNo(0);
        return dto;
    }

    private Game requireGame(long gameId) {
        Game game = gameMapper.findById(gameId);
        if (game == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "游戏不存在");
        return game;
    }

    private GamePlayer requirePlayer(long gameId, long userId) {
        GamePlayer player = gamePlayerMapper.findByGameAndUser(gameId, userId);
        if (player == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "你不属于该游戏");
        return player;
    }

    private Round requireRound(long gameId, int roundNo) {
        Round round = roundMapper.findByGameAndRoundNo(gameId, roundNo);
        if (round == null) throw new IllegalStateException("当前回合不存在");
        return round;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
