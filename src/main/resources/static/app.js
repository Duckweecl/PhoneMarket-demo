const API = {
    quickLogin: username => `/api/quick-login/${encodeURIComponent(username)}`,
    createGame: userId => `/${userId}/create`,
    joinGame: (userId, gameId) => `/${userId}/join/${gameId}`,
    startGame: (userId, gameId) => `/${userId}/start/${gameId}`,
    overview: (userId, gameId) => `/api/games/${gameId}/overview/${userId}`,
    status: (userId, gameId) => `/api/games/${gameId}/rounds/status/${userId}`,
    submitAction: (userId, gameId) => `/api/games/${gameId}/rounds/current/actions/${userId}`
};

const state = {
    userId: null,
    username: null,
    gameId: null,
    overview: null,
    activeTab: "market",
    pollTimer: null,
    loadingOverview: false,
    renderedRoundId: null
};

const segmentOrder = [
    "BUSINESS_FEMALE", "BUSINESS_MALE",
    "WORKER_FEMALE", "WORKER_MALE",
    "STUDENT_FEMALE", "STUDENT_MALE"
];

const segmentNames = {
    BUSINESS_MALE: "男性商务人士",
    BUSINESS_FEMALE: "女性商务人士",
    WORKER_MALE: "男性普通上班族",
    WORKER_FEMALE: "女性普通上班族",
    STUDENT_MALE: "男性学生",
    STUDENT_FEMALE: "女性学生"
};

const componentDefinitions = [
    {
        type: "SCREEN", label: "屏幕",
        options: {1: "基础 LCD 屏", 2: "高刷 OLED 屏", 3: "LTPO 旗舰屏"}
    },
    {
        type: "PROCESSOR", label: "处理器",
        options: {1: "入门级处理器", 2: "高性能处理器", 3: "旗舰级处理器"}
    },
    {
        type: "BODY", label: "机身",
        options: {1: "复合塑料机身", 2: "航空铝合金机身", 3: "钛合金机身"}
    },
    {
        type: "BATTERY", label: "电池",
        options: {1: "标准容量电池", 2: "大容量快充电池", 3: "高密度超快充电池"}
    },
    {
        type: "STORAGE", label: "存储",
        options: {1: "基础闪存", 2: "高速 UFS 闪存", 3: "旗舰 UFS 闪存"}
    },
    {
        type: "CAMERA", label: "相机",
        options: {1: "基础影像模组", 2: "光学防抖影像模组", 3: "旗舰多摄影像系统"}
    }
];

const advertisingDefinitions = [
    {field: "filmAd", label: "影视宣传", costPerPerson: 100},
    {field: "onlineAd", label: "网络宣传", costPerPerson: 25},
    {field: "magazineAd", label: "杂志宣传", costPerPerson: 10}
];

const playerPalette = ["#2563eb", "#7c3aed", "#0891b2", "#ea580c", "#16a34a", "#be123c"];
const systemBrandColor = "#64748b";

const elements = {};

document.addEventListener("DOMContentLoaded", initialize);

async function initialize() {
    cacheElements();
    bindEvents();
    renderAdvertisingOptions();

    const username = decodeURIComponent(location.pathname.replace(/^\//, "").trim());
    if (!username) {
        showMessage("请通过 /用户名 访问游戏。", "error");
        return;
    }

    try {
        const login = await request(API.quickLogin(username), {method: "POST"});
        state.userId = login.userId;
        state.username = login.username;
        elements.loginText.textContent = `当前用户：${login.username}（ID ${login.userId}）`;
        elements.createButton.disabled = false;
        elements.joinButton.disabled = false;
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function cacheElements() {
    [
        "loginText", "roomText", "refreshButton", "createButton", "joinButton", "startButton",
        "gameIdInput", "message", "gameArea", "processingBanner", "finalBanner", "summaryCards",
        "playerFinance", "consumerMarket", "starInfo", "componentTable", "previousRound", "rawJson",
        "componentSelectors", "modelName", "componentUnitCost", "assemblyUnitCost", "phoneUnitCost",
        "totalGrade", "majorBudgetCards", "productionQuantity", "salePrice", "starBid",
        "selectedComponentSupplyTable", "advertisingOptions", "operationUnitCost",
        "estimatedProductionCost", "estimatedAdCost", "estimatedStarCost", "maximumSpendingLimit",
        "estimatedTotalCost", "estimatedCashAfter", "estimatedAvailableAfter", "affordabilityWarning",
        "submitActionButton"
    ].forEach(id => elements[id] = document.getElementById(id));
}

function bindEvents() {
    elements.createButton.addEventListener("click", createGame);
    elements.joinButton.addEventListener("click", joinGame);
    elements.startButton.addEventListener("click", startGame);
    elements.refreshButton.addEventListener("click", loadOverview);
    elements.submitActionButton.addEventListener("click", submitAction);

    document.querySelectorAll(".tab-button").forEach(button => {
        button.addEventListener("click", () => switchTab(button.dataset.tab));
    });
    document.querySelectorAll("[data-go-tab]").forEach(button => {
        button.addEventListener("click", () => switchTab(button.dataset.goTab));
    });

    [elements.modelName, elements.productionQuantity, elements.salePrice, elements.starBid]
        .forEach(input => input.addEventListener("input", () => {
            updateCalculations();
            saveDraft();
        }));
}

async function createGame() {
    try {
        const result = await request(API.createGame(state.userId), {method: "POST"});
        enterRoom(result.game.id);
        showMessage(`房间 ${result.game.id} 已创建。`, "success");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function joinGame() {
    const gameId = positiveInteger(elements.gameIdInput.value);
    if (!gameId) return showMessage("请输入有效房间 ID。", "error");
    try {
        const result = await request(API.joinGame(state.userId, gameId), {method: "POST"});
        enterRoom(result.game.id);
        showMessage(`已加入房间 ${result.game.id}。`, "success");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function enterRoom(gameId) {
    state.gameId = Number(gameId);
    elements.roomText.textContent = state.gameId;
    elements.gameIdInput.value = state.gameId;
    elements.startButton.disabled = false;
    elements.refreshButton.disabled = false;
}

async function startGame() {
    if (!state.gameId) return showMessage("请先创建或加入房间。", "error");
    try {
        const overview = await request(API.startGame(state.userId, state.gameId), {method: "POST"});
        applyOverview(overview);
        showMessage("游戏已经开始。", "success");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadOverview() {
    if (!state.userId || !state.gameId || state.loadingOverview) return;
    state.loadingOverview = true;
    try {
        const overview = await request(API.overview(state.userId, state.gameId));
        applyOverview(overview);
    } catch (error) {
        showMessage(error.message, "error");
    } finally {
        state.loadingOverview = false;
    }
}

function applyOverview(overview) {
    const oldRoundId = state.overview?.currentRound?.roundId;
    state.overview = overview;
    state.gameId = overview.gameId;
    elements.roomText.textContent = overview.gameId;
    elements.gameArea.classList.remove("hidden");
    elements.refreshButton.disabled = false;

    renderSummary();
    renderPlayers();
    renderConsumerMarket();
    renderStar();
    renderComponents();
    renderPreviousRound();
    renderMajorBudgets();
    renderFinalState();
    elements.rawJson.textContent = JSON.stringify(overview, null, 2);

    renderComponentSelectors();
    if (oldRoundId !== overview.currentRound?.roundId || state.renderedRoundId !== overview.currentRound?.roundId) {
        state.renderedRoundId = overview.currentRound?.roundId;
        loadDraft();
    }
    updateInteractionState();
    updateCalculations();
    startPolling();
}

function renderSummary() {
    const o = state.overview;
    const round = o.currentRound || {};
    const submitted = number(o.submittedCount);
    const expected = number(o.expectedPlayerCount);
    elements.summaryCards.innerHTML = [
        summaryCard("游戏状态", gameStatusName(o.gameStatus)),
        summaryCard("当前回合", `${o.currentRoundNo} / ${o.maxRound}`),
        summaryCard("回合状态", roundStatusName(round.status)),
        summaryCard("提交进度", `${submitted} / ${expected}`),
        summaryCard("你的排名", o.currentPlayer?.rank ? `第 ${o.currentPlayer.rank} 名` : "-")
    ].join("");
    elements.processingBanner.classList.toggle("hidden", round.status !== "PROCESSING");
}

function renderPlayers() {
    const players = state.overview?.players || [];
    elements.playerFinance.innerHTML = `<div class="player-grid">${players.map(player => {
        const self = Boolean(player.currentPlayer);
        return `<article class="player-card ${self ? "current-player-card" : ""}">
            <div class="player-card-title">
                <div><span class="rank-badge">#${escapeHtml(player.rank)}</span> ${escapeHtml(player.username)}</div>
                ${self ? '<span class="self-badge">你</span>' : ""}
            </div>
            <div class="player-status-line">${escapeHtml(player.status || "-")}</div>
            <dl class="player-finance-list">
                <div><dt>现金</dt><dd class="positive">${formatMoney(player.cash)}</dd></div>
                <div><dt>当前负债</dt><dd class="negative">${formatMoney(player.debt)}</dd></div>
                <div><dt>贷款上限</dt><dd>${formatMoney(player.debtLimit)}</dd></div>
                <div><dt>可用贷款额度</dt><dd class="positive">${formatMoney(player.availableCredit)}</dd></div>
                <div><dt>累计销售利润</dt><dd class="${profitClass(player.cumulativeSalesProfit)}">${formatSignedMoney(player.cumulativeSalesProfit)}</dd></div>
            </dl>
        </article>`;
    }).join("")}</div>`;
}

function renderConsumerMarket() {
    const round = state.overview?.currentRound || {};
    const segments = round.segments || [];
    const cohorts = round.consumerCohorts || [];
    const holdings = round.segmentHoldings || [];

    elements.consumerMarket.innerHTML = `<div class="consumer-grid">${segmentOrder.map(code => {
        const segment = segments.find(item => item.segmentCode === code);
        if (!segment) return "";
        const segmentCohorts = cohorts.filter(item => item.segmentCode === code);
        const holding = holdings.find(item => item.segmentCode === code);
        return `<article class="consumer-card">
            <div class="consumer-card-header">
                <div>
                    <h3>${escapeHtml(segmentNames[code] || code)}</h3>
                    <p>${formatInteger(segment.population)} 人 · 人均预算 <strong>${formatMoney(segment.averageBudget)}</strong></p>
                </div>
            </div>
            ${renderBrandMarket(holding, segmentCohorts)}
        </article>`;
    }).join("")}</div>`;
}

function renderBrandMarket(holding, segmentCohorts) {
    const companies = holding?.companyHoldings || [];
    if (!companies.length) return '<p class="empty-text">暂无品牌占比</p>';

    let cursor = 0;
    const slices = companies.map(company => {
        const percentage = Math.max(0, number(company.holdingRate) * 100);
        const start = cursor;
        const end = cursor + percentage;
        cursor = end;
        return `${companyColor(company)} ${start}% ${end}%`;
    });

    const companyRows = companies.map(company => {
        const color = companyColor(company);
        const models = company.models || [];
        return `<details class="brand-company-item" style="--company-color:${color}">
            <summary>
                <span class="legend-dot"></span>
                <span class="brand-company-name">${escapeHtml(company.companyName || "系统品牌")}</span>
                <span class="brand-company-population">${formatInteger(company.holdingPopulation)} 人</span>
                <strong>${formatPercent(number(company.holdingRate) * 100)}</strong>
                <span class="details-arrow" aria-hidden="true">⌄</span>
            </summary>
            <div class="brand-model-list">
                ${models.map(model => renderBrandModel(model, segmentCohorts, color)).join("") || '<p class="empty-text">暂无手机款式</p>'}
            </div>
        </details>`;
    }).join("");

    return `<div class="brand-market-layout">
        <div class="pie-chart" style="--pie: conic-gradient(${slices.join(",")})" aria-label="品牌占比饼图"></div>
        <div class="brand-company-list">${companyRows}</div>
    </div>`;
}

function renderBrandModel(model, segmentCohorts, color) {
    const matching = (segmentCohorts || []).filter(cohort =>
        Number(cohort.phoneModelId) === Number(model.phoneModelId)
    );
    const cohortDetails = matching.map(cohort =>
        `使用 ${formatInteger(cohort.usedRounds)} 回合，grade：${formatInteger(cohort.totalGrade)} · ${formatInteger(cohort.population)} 人`
    ).join("；");

    return `<div class="brand-model-row">
        <span class="model-color-line" style="background:${color}"></span>
        <div class="brand-model-copy">
            <strong>${escapeHtml(model.modelName || "未命名机型")}</strong>
            <small>${escapeHtml(cohortDetails || "暂无批次明细")}</small>
        </div>
        <div class="brand-model-metrics">
            <span>${formatInteger(model.population)} 人</span>
            <strong>${formatPercent(number(model.holdingRate) * 100)}</strong>
        </div>
    </div>`;
}

function companyColor(company) {
    if (company?.gamePlayerId == null) return systemBrandColor;
    const player = (state.overview?.players || []).find(item =>
        Number(item.gamePlayerId) === Number(company.gamePlayerId)
    );
    const stableIndex = player?.seatNo != null
        ? Math.max(0, Number(player.seatNo) - 1)
        : Math.abs(Number(company.gamePlayerId) || 0);
    return playerPalette[stableIndex % playerPalette.length];
}

function renderStar() {
    const star = state.overview?.currentRound?.star;
    if (!star) {
        elements.starInfo.innerHTML = '<p class="empty-text">暂无明星信息</p>';
        return;
    }
    if (!star.settled) {
        elements.starInfo.innerHTML = `<div class="star-card">
            <div><span>目标人群</span><strong>${escapeHtml(segmentNames[star.targetSegmentCode] || star.targetSegmentCode)}</strong></div>
            <div><span>宣传力度</span><strong>结算前隐藏</strong></div>
            <div><span>最终归属</span><strong>等待结算</strong></div>
        </div>`;
        return;
    }
    elements.starInfo.innerHTML = `<div class="star-card">
        <div><span>目标人群</span><strong>${escapeHtml(segmentNames[star.targetSegmentCode] || star.targetSegmentCode)}</strong></div>
        <div><span>全体加成</span><strong>+${formatDecimal(star.boost)}</strong></div>
        <div><span>目标人群加成</span><strong>+${formatDecimal(star.targetSegmentBoost)}</strong></div>
        <div><span>最终归属</span><strong>${escapeHtml(star.winnerCompanyName || "无人签约")}</strong></div>
    </div>`;
}

function renderComponents() {
    const markets = state.overview?.currentRound?.componentMarkets || [];

    elements.componentTable.innerHTML = componentDefinitions.map(definition => {
        const rows = markets
            .filter(market => market.componentType === definition.type)
            .sort((a, b) => number(a.componentLevel) - number(b.componentLevel));

        return `<article class="component-market-card">
            <div class="component-market-card-title">
                <h3>${escapeHtml(definition.label)}</h3>
            </div>
            <div class="component-table-scroll">
                <table class="component-market-table">
                    <thead>
                        <tr>
                            <th>方案</th>
                            <th>当前价格</th>
                            <th>上回合需求</th>
                            <th>上回合供货</th>
                            <th>本回合供货</th>
                            <th>供货变化</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rows.map(market => {
                            const change = market.supplyChange;
                            const trendClass = number(change) > 0 ? "market-up" : number(change) < 0 ? "market-down" : "market-flat";
                            return `<tr>
                                <td>${escapeHtml(componentOptionName(market.componentType, market.componentLevel))}</td>
                                <td>${formatMoney(market.actualUnitPrice)}</td>
                                <td>${nullableInteger(market.previousDemandQuantity)}</td>
                                <td>${nullableInteger(market.previousSupplyQuantity)}</td>
                                <td>${formatInteger(market.supplyQuantity)}</td>
                                <td class="${trendClass}">${change == null ? "-" : formatSignedInteger(change)}</td>
                            </tr>`;
                        }).join("")}
                    </tbody>
                </table>
            </div>
        </article>`;
    }).join("");
}

function renderPreviousRound() {
    const previous = state.overview?.previousRound;
    if (!previous || number(previous.roundNo) === 0) {
        elements.previousRound.innerHTML = '<p class="empty-text">第一回合尚无历史结算结果。</p>';
        return;
    }

    const star = previous.starResult;
    const starHtml = star ? `<section class="result-section">
        <h3>明星结算</h3>
        <div class="star-result-grid">
            <div><span>目标人群</span><strong>${escapeHtml(segmentNames[star.targetSegmentCode] || star.targetSegmentCode)}</strong></div>
            <div><span>全体宣传力度</span><strong>+${formatDecimal(star.boost)}</strong></div>
            <div><span>目标人群力度</span><strong>+${formatDecimal(star.targetSegmentBoost)}</strong></div>
            <div><span>最终归属</span><strong>${escapeHtml(star.winnerCompanyName || "无人签约")}</strong></div>
            <div><span>中标价格</span><strong>${formatMoney(star.winningBid)}</strong></div>
        </div>
        <div class="bid-list">${(star.bids || []).map(bid => `<span>${escapeHtml(bid.companyName)}：${formatMoney(bid.bid)}</span>`).join("")}</div>
    </section>` : "";

    const results = previous.playerResults || [];
    const publicHtml = `<section class="result-section">
        <h3>全部玩家公开经营结果</h3>
        <div class="table-wrap"><table><thead><tr>
            <th>玩家</th><th>手机</th><th>模块方案</th><th>定价</th><th>产量</th><th>销量</th><th>未售</th><th>宣传策略</th><th>明星报价</th><th>销售利润</th>
        </tr></thead><tbody>${results.map(result => `<tr>
            <td>${escapeHtml(result.companyName)}</td>
            <td>${escapeHtml(result.phoneModel?.modelName || "-")}<br><small>grade ${formatInteger(result.phoneModel?.totalGrade)}</small></td>
            <td class="module-cell">${renderModuleSummary(result.phoneModel)}</td>
            <td>${formatMoney(result.salePrice)}</td>
            <td>${formatInteger(result.productionQuantity)}</td>
            <td>${formatInteger(result.consumerSalesQuantity)}</td>
            <td>${formatInteger(result.unsoldQuantity)}</td>
            <td>${renderAdSummary(result)}</td>
            <td>${formatMoney(result.starBid)}${result.wonStar ? '<br><span class="winner-badge">中标</span>' : ""}</td>
            <td class="${profitClass(result.salesProfit)}">${formatSignedMoney(result.salesProfit)}</td>
        </tr>`).join("")}</tbody></table></div>
    </section>`;

    const finance = previous.currentPlayerFinancialResult;
    const financeHtml = finance ? `<section class="result-section private-finance">
        <div class="section-heading"><h3>你的完整财务报告</h3><span class="private-badge">仅你可见</span></div>
        <div class="finance-columns">
            ${financeGroup("期初与经营支出", [
                ["期初现金", finance.beginningCash, "positive"],
                ["期初负债", finance.beginningDebt, "negative"],
                ["期初可用额度", finance.beginningAvailableCredit, "positive"],
                ["生产数量", finance.productionQuantity, "neutral", "integer"],
                ["单台零部件成本", finance.componentUnitCost, "negative"],
                ["零部件成本", finance.componentCost, "negative"],
                ["单台组装成本", finance.assemblyUnitCost, "negative"],
                ["组装成本", finance.assemblyCost, "negative"],
                ["生产成本", finance.productionCost, "negative"],
                ["影视宣传费", finance.filmAdvertisingCost, "negative"],
                ["网络宣传费", finance.onlineAdvertisingCost, "negative"],
                ["杂志宣传费", finance.magazineAdvertisingCost, "negative"],
                ["宣传总费用", finance.advertisingCost, "negative"],
                ["明星报价", finance.starBid, "neutral"],
                ["明星签约费", finance.starCost, "negative"],
                ["总经营支出", finance.totalOperatingCost, "negative"]
            ])}
            ${financeGroup("收入与库存", [
                ["销售定价", finance.salePrice, "neutral"],
                ["消费者销量", finance.consumerSalesQuantity, "neutral", "integer"],
                ["未售数量", finance.unsoldQuantity, "neutral", "integer"],
                ["消费者销售收入", finance.consumerSalesRevenue, "positive"],
                ["回收单价", finance.liquidationUnitPrice, "neutral"],
                ["未售回收收入", finance.liquidationRevenue, "positive"],
                ["总收入", finance.totalRevenue, "positive"],
                ["公开销售利润", finance.salesProfit, profitClass(finance.salesProfit)]
            ])}
            ${financeGroup("贷款与期末", [
                ["新增普通贷款", finance.newNormalLoan, "negative"],
                ["普通贷款本金", finance.normalLoanPrincipal, "negative"],
                ["普通贷款利息", finance.normalLoanInterest, "negative"],
                ["高息资金本金", finance.paydayPrincipal, "negative"],
                ["高息资金利息", finance.paydayInterest, "negative"],
                ["应还贷款总额", finance.totalRepaymentDue, "negative"],
                ["自动偿还金额", finance.actualRepayment, "negative"],
                ["期末现金", finance.endingCash, "positive"],
                ["期末负债", finance.endingDebt, "negative"],
                ["期末可用额度", finance.endingAvailableCredit, "positive"],
                ["真实现金结果", finance.roundCashResult, profitClass(finance.roundCashResult)],
                ["本回合结算利润", finance.roundSettlementProfit, profitClass(finance.roundSettlementProfit)],
                ["累计销售利润", finance.endingCumulativeSalesProfit, profitClass(finance.endingCumulativeSalesProfit)],
                ["累计结算利润", finance.endingTotalSettlementProfit, profitClass(finance.endingTotalSettlementProfit)]
            ])}
        </div>
    </section>` : "";

    elements.previousRound.innerHTML = `<p class="round-label">第 ${formatInteger(previous.roundNo)} 回合</p>${starHtml}${publicHtml}${financeHtml}`;
}

function financeGroup(title, rows) {
    return `<div class="finance-group"><h4>${escapeHtml(title)}</h4><dl>${rows.map(([label, value, tone, format]) => `
        <div><dt>${escapeHtml(label)}</dt><dd class="${tone === "neutral" ? "" : tone}">${format === "integer" ? formatInteger(value) : formatMoney(value)}</dd></div>`).join("")}</dl></div>`;
}

function renderMajorBudgets() {
    const segments = state.overview?.currentRound?.segments || [];
    const groups = [
        ["商务人群", ["BUSINESS_FEMALE", "BUSINESS_MALE"]],
        ["上班族", ["WORKER_FEMALE", "WORKER_MALE"]],
        ["学生", ["STUDENT_FEMALE", "STUDENT_MALE"]]
    ];
    elements.majorBudgetCards.innerHTML = groups.map(([label, codes]) => {
        const values = segments.filter(s => codes.includes(s.segmentCode)).map(s => number(s.averageBudget));
        const budget = values.length ? values.reduce((a, b) => a + b, 0) / values.length : 0;
        return summaryCard(label, formatMoney(budget));
    }).join("");
}

function renderFinalState() {
    const finished = Boolean(state.overview?.gameFinished);
    elements.finalBanner.classList.toggle("hidden", !finished);
    document.querySelectorAll('.tab-button[data-tab="research"], .tab-button[data-tab="operation"]')
        .forEach(button => button.disabled = finished);
    if (!finished) return;
    const winners = (state.overview.players || []).filter(player => number(player.rank) === 1);
    elements.finalBanner.innerHTML = `<p class="eyebrow">GAME FINISHED</p>
        <h2>游戏结束</h2>
        <p>并列第一：${winners.map(w => escapeHtml(w.username)).join("、") || "-"}</p>
        <p>最终排名以累计结算利润为准。利润完全相同的玩家并列。</p>`;
    switchTab("market");
}

function renderComponentSelectors() {
    const existing = Object.fromEntries(componentDefinitions.map(def => [
        def.type,
        Number(document.querySelector(`[data-component="${def.type}"]`)?.value || 1)
    ]));
    elements.componentSelectors.innerHTML = componentDefinitions.map(definition => `<label class="field">
        ${escapeHtml(definition.label)}
        <select data-component="${definition.type}">
            ${[1, 2, 3].map(level => `<option value="${level}" ${existing[definition.type] === level ? "selected" : ""}>
                ${escapeHtml(definition.options[level])} — ${formatMoney(getComponentPrice(definition.type, level))}
            </option>`).join("")}
        </select>
    </label>`).join("");
    elements.componentSelectors.querySelectorAll("select").forEach(select => {
        select.addEventListener("change", () => {
            updateCalculations();
            saveDraft();
        });
    });
}

function renderAdvertisingOptions() {
    elements.advertisingOptions.innerHTML = advertisingDefinitions.map(ad => `<label class="checkbox-option">
        <input type="checkbox" data-ad-field="${ad.field}">
        <span><strong>${ad.label}</strong><small>全体宣传加成 +0.20 · 每名消费者成本 ${formatMoney(ad.costPerPerson)}</small></span>
    </label>`).join("");
    elements.advertisingOptions.querySelectorAll("input").forEach(input => {
        input.addEventListener("change", () => {
            updateCalculations();
            saveDraft();
        });
    });
}

function updateCalculations() {
    if (!state.overview) return;
    const levels = getSelectedLevels();
    const componentUnitCost = componentDefinitions.reduce(
        (sum, def) => sum + getComponentPrice(def.type, levels[def.type]), 0
    );
    const assemblyUnitCost = calculateAssemblyCost(Object.values(levels));
    const phoneUnitCost = componentUnitCost + assemblyUnitCost;
    const totalGrade = Object.values(levels).reduce((sum, level) => sum + level, 0);
    const productionQuantity = Math.max(0, integer(elements.productionQuantity.value));
    const productionCost = phoneUnitCost * productionQuantity;
    const population = getCityPopulation();
    const adCost = advertisingDefinitions.reduce((sum, ad) => {
        const selected = document.querySelector(`[data-ad-field="${ad.field}"]`)?.checked;
        return selected ? sum + population * ad.costPerPerson : sum;
    }, 0);
    const starBid = Math.max(0, integer(elements.starBid.value));
    const totalCost = productionCost + adCost + starBid;
    const cash = number(state.overview.currentPlayer?.cash);
    const availableCredit = number(state.overview.currentPlayer?.availableCredit);
    const maximum = cash + availableCredit;

    elements.componentUnitCost.textContent = formatMoney(componentUnitCost);
    elements.assemblyUnitCost.textContent = formatMoney(assemblyUnitCost);
    elements.phoneUnitCost.textContent = formatMoney(phoneUnitCost);
    elements.totalGrade.textContent = formatInteger(totalGrade);
    elements.operationUnitCost.textContent = formatMoney(phoneUnitCost);
    elements.estimatedProductionCost.textContent = formatMoney(productionCost);
    elements.estimatedAdCost.textContent = formatMoney(adCost);
    elements.estimatedStarCost.textContent = formatMoney(starBid);
    elements.maximumSpendingLimit.textContent = formatMoney(maximum);
    elements.estimatedTotalCost.textContent = formatMoney(totalCost);
    elements.estimatedCashAfter.innerHTML = semanticMoney(cash - totalCost, cash - totalCost >= 0 ? "positive" : "negative");
    elements.estimatedAvailableAfter.innerHTML = semanticMoney(maximum - totalCost, maximum - totalCost >= 0 ? "positive" : "negative");

    const unaffordable = totalCost > maximum;
    elements.affordabilityWarning.classList.toggle("hidden", !unaffordable);
    elements.affordabilityWarning.textContent = unaffordable
        ? `计划支出超过最大支出范围 ${formatMoney(totalCost - maximum)}，无法提交。`
        : "";

    renderSelectedSupply(levels, productionQuantity);
    elements.submitActionButton.disabled = unaffordable || interactionLocked();
}

function renderSelectedSupply(levels, productionQuantity) {
    elements.selectedComponentSupplyTable.innerHTML = componentDefinitions.map(def => {
        const market = getComponentMarket(def.type, levels[def.type]);
        const remaining = number(market?.supplyQuantity) - productionQuantity;
        return `<tr>
            <td>${def.label}</td>
            <td>${escapeHtml(def.options[levels[def.type]])}</td>
            <td>${formatInteger(market?.supplyQuantity)}</td>
            <td>${formatInteger(productionQuantity)}</td>
            <td class="${remaining < 0 ? "negative" : ""}">${formatSignedInteger(remaining)}</td>
        </tr>`;
    }).join("");
}

async function submitAction() {
    if (interactionLocked()) return;
    const payload = collectActionPayload();
    if (!payload.modelName) return showMessage("请输入手机型号名称。", "error");
    if (payload.salePrice <= 0) return showMessage("售价必须大于 0。", "error");

    elements.submitActionButton.disabled = true;
    showMessage("正在提交方案……", "");
    try {
        const response = await request(API.submitAction(state.userId, state.gameId), {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        showMessage(response.message || "提交成功。", "success");
        if (state.overview && response.roundStatus === "PROCESSING") {
            state.overview.submittedCount = response.submittedCount;
            state.overview.expectedPlayerCount = response.expectedPlayerCount;
            state.overview.currentPlayerSubmitted = true;
            state.overview.currentRound.status = "PROCESSING";
            renderSummary();
            updateInteractionState();
            await sleep(300);
        }
        await loadOverview();
    } catch (error) {
        showMessage(error.message, "error");
    } finally {
        updateInteractionState();
        updateCalculations();
    }
}

function collectActionPayload() {
    const levels = getSelectedLevels();
    return {
        modelName: elements.modelName.value.trim(),
        screenLevel: levels.SCREEN,
        processorLevel: levels.PROCESSOR,
        bodyLevel: levels.BODY,
        batteryLevel: levels.BATTERY,
        storageLevel: levels.STORAGE,
        cameraLevel: levels.CAMERA,
        productionQuantity: Math.max(0, integer(elements.productionQuantity.value)),
        salePrice: Math.max(0, integer(elements.salePrice.value)),
        filmAd: Boolean(document.querySelector('[data-ad-field="filmAd"]')?.checked),
        onlineAd: Boolean(document.querySelector('[data-ad-field="onlineAd"]')?.checked),
        magazineAd: Boolean(document.querySelector('[data-ad-field="magazineAd"]')?.checked),
        starBid: Math.max(0, integer(elements.starBid.value))
    };
}

function updateInteractionState() {
    const locked = interactionLocked();
    document.querySelectorAll("#researchTab input, #researchTab select, #operationTab input")
        .forEach(control => control.disabled = locked);
    elements.submitActionButton.disabled = locked;
    elements.processingBanner.classList.toggle("hidden", state.overview?.currentRound?.status !== "PROCESSING");
}

function interactionLocked() {
    return !state.overview
        || Boolean(state.overview.gameFinished)
        || Boolean(state.overview.currentPlayerSubmitted)
        || state.overview.currentRound?.status !== "COLLECTING";
}

function startPolling() {
    if (state.pollTimer) return;
    state.pollTimer = setInterval(pollStatus, 1000);
}

async function pollStatus() {
    if (!state.overview || !state.gameId || state.loadingOverview) return;
    try {
        const status = await request(API.status(state.userId, state.gameId));
        const current = state.overview.currentRound;
        const roundChanged = Number(status.currentRoundId) !== Number(current?.roundId);
        const gameChanged = status.gameStatus !== state.overview.gameStatus;
        const statusChanged = status.roundStatus !== current?.status;

        state.overview.submittedCount = status.submittedCount;
        state.overview.expectedPlayerCount = status.expectedPlayerCount;
        state.overview.currentPlayerSubmitted = status.currentPlayerSubmitted;
        if (!roundChanged) state.overview.currentRound.status = status.roundStatus;
        renderSummary();
        updateInteractionState();

        if (roundChanged || gameChanged || (statusChanged && status.roundStatus !== "PROCESSING")) {
            await loadOverview();
        }
    } catch (error) {
        console.debug("状态轮询失败", error.message);
    }
}

function switchTab(tabName) {
    if (state.overview?.gameFinished && tabName !== "market") tabName = "market";
    state.activeTab = tabName;
    document.querySelectorAll(".tab-button").forEach(button => {
        button.classList.toggle("active", button.dataset.tab === tabName);
    });
    document.querySelectorAll(".tab-page").forEach(page => page.classList.add("hidden"));
    document.getElementById(`${tabName}Tab`).classList.remove("hidden");
    updateCalculations();
}

function saveDraft() {
    if (!state.userId || !state.gameId || !state.overview?.currentRound?.roundId) return;
    sessionStorage.setItem(draftStorageKey(), JSON.stringify(collectActionPayload()));
}

function loadDraft() {
    const raw = sessionStorage.getItem(draftStorageKey());
    if (!raw) {
        elements.modelName.value = "";
        elements.productionQuantity.value = 0;
        elements.salePrice.value = 1;
        elements.starBid.value = 0;
        document.querySelectorAll('[data-ad-field]').forEach(item => item.checked = false);
        updateCalculations();
        return;
    }
    try {
        const draft = JSON.parse(raw);
        elements.modelName.value = draft.modelName || "";
        setComponentLevel("SCREEN", draft.screenLevel);
        setComponentLevel("PROCESSOR", draft.processorLevel);
        setComponentLevel("BODY", draft.bodyLevel);
        setComponentLevel("BATTERY", draft.batteryLevel);
        setComponentLevel("STORAGE", draft.storageLevel);
        setComponentLevel("CAMERA", draft.cameraLevel);
        elements.productionQuantity.value = draft.productionQuantity ?? 0;
        elements.salePrice.value = draft.salePrice ?? 1;
        elements.starBid.value = draft.starBid ?? 0;
        document.querySelector('[data-ad-field="filmAd"]').checked = Boolean(draft.filmAd);
        document.querySelector('[data-ad-field="onlineAd"]').checked = Boolean(draft.onlineAd);
        document.querySelector('[data-ad-field="magazineAd"]').checked = Boolean(draft.magazineAd);
    } catch (error) {
        console.debug("草稿读取失败", error);
    }
    updateCalculations();
}

function draftStorageKey() {
    return `phonemarket:draft:${state.gameId}:${state.overview?.currentRound?.roundId}:${state.userId}`;
}

function setComponentLevel(type, level) {
    const select = document.querySelector(`[data-component="${type}"]`);
    if (select && [1, 2, 3].includes(Number(level))) select.value = String(level);
}

function getSelectedLevels() {
    return Object.fromEntries(componentDefinitions.map(def => [
        def.type,
        Number(document.querySelector(`[data-component="${def.type}"]`)?.value || 1)
    ]));
}

function getComponentMarket(type, level) {
    return (state.overview?.currentRound?.componentMarkets || [])
        .find(item => item.componentType === type && Number(item.componentLevel) === Number(level));
}

function getComponentPrice(type, level) {
    const item = getComponentMarket(type, level);
    return number(item?.actualUnitPrice ?? item?.basePrice);
}

function componentOptionName(type, level) {
    return componentDefinitions.find(def => def.type === type)?.options?.[Number(level)] || `${type}-${level}`;
}

function calculateAssemblyCost(levels) {
    const level3 = levels.filter(level => Number(level) === 3).length;
    const level2Plus = levels.filter(level => Number(level) >= 2).length;
    if (level3 >= 2) return 2000;
    if (level2Plus >= 2) return 1000;
    return 500;
}

function getCityPopulation() {
    return (state.overview?.currentRound?.segments || [])
        .reduce((sum, segment) => sum + number(segment.population), 0);
}

function renderModuleSummary(model) {
    if (!model) return "-";
    const levels = {
        SCREEN: model.screenLevel,
        PROCESSOR: model.processorLevel,
        BODY: model.bodyLevel,
        BATTERY: model.batteryLevel,
        STORAGE: model.storageLevel,
        CAMERA: model.cameraLevel
    };
    return componentDefinitions.map(def => escapeHtml(def.options[Number(levels[def.type])])).join("<br>");
}

function renderAdSummary(result) {
    const ads = [];
    if (result.filmAd) ads.push("影视");
    if (result.onlineAd) ads.push("网络");
    if (result.magazineAd) ads.push("杂志");
    return ads.length ? ads.join("、") : "无";
}

function summaryCard(label, value) {
    return `<div class="summary-card"><span>${escapeHtml(label)}</span><strong>${escapeHtml(String(value))}</strong></div>`;
}

function semanticMoney(value, tone) {
    return `<span class="${tone}">${formatSignedMoney(value)}</span>`;
}

function profitClass(value) {
    const n = number(value);
    return n > 0 ? "positive" : n < 0 ? "negative" : "";
}

function gameStatusName(status) {
    return {WAITING: "等待开始", RUNNING: "进行中", FINISHED: "已结束", ABORTED: "已解散"}[status] || status || "-";
}

function roundStatusName(status) {
    return {COLLECTING: "等待提交", PROCESSING: "市场结算中", FINISHED: "已完成"}[status] || status || "-";
}

function formatMoney(value) {
    return `¥${number(value).toLocaleString("zh-CN", {minimumFractionDigits: 0, maximumFractionDigits: 2})}`;
}

function formatSignedMoney(value) {
    const n = number(value);
    const sign = n > 0 ? "+" : "";
    return `${sign}¥${n.toLocaleString("zh-CN", {minimumFractionDigits: 0, maximumFractionDigits: 2})}`;
}

function formatInteger(value) {
    if (value === null || value === undefined) return "-";
    return integer(value).toLocaleString("zh-CN");
}

function nullableInteger(value) {
    return value === null || value === undefined ? "-" : formatInteger(value);
}

function formatSignedInteger(value) {
    const n = integer(value);
    return `${n > 0 ? "+" : ""}${n.toLocaleString("zh-CN")}`;
}

function formatPercent(value) {
    return `${number(value).toFixed(1)}%`;
}

function formatDecimal(value) {
    return number(value).toFixed(2);
}

function number(value) {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
}

function integer(value) {
    return Math.trunc(number(value));
}

function positiveInteger(value) {
    const n = integer(value);
    return n > 0 ? n : null;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function request(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();
    let body = null;
    if (text) {
        try { body = JSON.parse(text); } catch { body = text; }
    }
    if (!response.ok) {
        const message = body?.message || body?.detail || (typeof body === "string" ? body : `请求失败：${response.status}`);
        throw new Error(message);
    }
    return body;
}

function showMessage(text, type = "") {
    elements.message.textContent = text || "";
    elements.message.className = `message ${type}`.trim();
}
