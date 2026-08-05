"use strict";

const API = {
    currentUser: "/api/auth/me",
    logout: "/api/auth/logout",
    room: gameId => `/api/games/${gameId}/room`,
    start: gameId => `/api/games/${gameId}/start`,
    leave: gameId => `/api/games/${gameId}/leave`,
    abort: gameId => `/api/games/${gameId}/abort`
};

const state = {
    userId: null,
    gameId: null,
    roomAndPlayers: null,
    currentPlayer: null,
    ownerPlayer: null,
    pollingTimer: null,
    loading: false,
    redirecting: false,
    messageCloseAction: null
};

const elements = Object.fromEntries([
    "nicknameText",
    "homeButton",
    "logoutButton",
    "gameIdText",
    "roomDescription",
    "statusBadge",
    "playerCountText",
    "playerList",
    "emptyPlayerText",
    "roomStatusText",
    "ownerText",
    "currentPlayersText",
    "currentRoundText",
    "maxRoundText",
    "refreshButton",
    "startGameButton",
    "leaveRoomButton",
    "actionHint",
    "messageModal",
    "messageTitle",
    "messageText",
    "closeMessageButton"
].map(id => [id, document.getElementById(id)]));

async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            Accept: "application/json",
            ...(options.headers || {})
        }
    });

    const text = await response.text();
    let body = {};

    if (text) {
        try {
            body = JSON.parse(text);
        } catch {
            body = {message: text};
        }
    }

    if (!response.ok || body.success === false) {
        const error = new Error(
            body.message || body.detail || `请求失败：${response.status}`
        );
        error.status = response.status;
        throw error;
    }

    return body;
}

function showMessage(title, text, closeAction = null) {
    state.messageCloseAction = closeAction;
    elements.messageTitle.textContent = title;
    elements.messageText.textContent = text;
    elements.messageModal.classList.remove("hidden");
}

function closeMessage() {
    elements.messageModal.classList.add("hidden");
    const closeAction = state.messageCloseAction;
    state.messageCloseAction = null;
    if (closeAction) closeAction();
}

function getGameIdFromUrl() {
    const gameId = Number(new URLSearchParams(location.search).get("gameId"));
    return Number.isInteger(gameId) && gameId > 0 ? gameId : null;
}

function gameUrl() {
    return `/game.html?gameId=${encodeURIComponent(state.gameId)}`;
}

function stopPolling() {
    if (state.pollingTimer) {
        clearInterval(state.pollingTimer);
        state.pollingTimer = null;
    }
}

function statusText(status) {
    return {
        WAITING: "等待开始",
        RUNNING: "比赛进行中",
        FINISHED: "比赛已结束",
        ABORTED: "房间已解散"
    }[status] || status || "未知状态";
}

function updateStatusBadge(status) {
    elements.statusBadge.className = "status-badge";

    const className = {
        WAITING: "waiting",
        RUNNING: "running",
        FINISHED: "finished",
        ABORTED: "aborted"
    }[status] || "finished";

    elements.statusBadge.classList.add(className);
    elements.statusBadge.textContent = statusText(status);
}

function playerDisplayName(player) {
    return player.nickname || player.username || `玩家 ${player.userId}`;
}

function createLabel(text, className) {
    const label = document.createElement("span");
    label.className = className;
    label.textContent = text;
    return label;
}

function createPlayerCard(player) {
    const card = document.createElement("article");
    card.className = "player-card";
    if (player.status !== "ACTIVE") card.classList.add("inactive");

    const main = document.createElement("div");
    main.className = "player-main";

    const avatar = document.createElement("div");
    avatar.className = "player-avatar";
    avatar.textContent = player.seatNo ?? "?";

    const info = document.createElement("div");
    const name = document.createElement("div");
    name.className = "player-name";
    name.textContent = playerDisplayName(player);

    const extra = document.createElement("div");
    extra.className = "player-extra";
    extra.textContent = `${player.username ? `@${player.username} · ` : ""}座位 ${player.seatNo ?? "-"}`;

    info.appendChild(name);
    info.appendChild(extra);
    main.appendChild(avatar);
    main.appendChild(info);

    const labels = document.createElement("div");
    labels.className = "player-labels";

    if (Number(player.seatNo) === 1) {
        labels.appendChild(createLabel("房主", "owner-label"));
    }

    if (Number(player.userId) === state.userId) {
        labels.appendChild(createLabel("你", "current-user-label"));
    }

    labels.appendChild(createLabel(
        player.status === "ACTIVE" ? "房间中" : "已离开",
        `player-status-label ${player.status === "ACTIVE" ? "" : "inactive"}`.trim()
    ));

    card.appendChild(main);
    card.appendChild(labels);
    return card;
}

function renderPlayers(players) {
    elements.playerList.replaceChildren();

    if (!players.length) {
        elements.emptyPlayerText.classList.remove("hidden");
        return;
    }

    elements.emptyPlayerText.classList.add("hidden");

    [...players]
        .sort((a, b) => Number(a.seatNo) - Number(b.seatNo))
        .forEach(player => elements.playerList.appendChild(createPlayerCard(player)));
}

function renderActions(game, players) {
    state.currentPlayer = players.find(player => Number(player.userId) === state.userId) || null;
    state.ownerPlayer = players.find(player => Number(player.seatNo) === 1) || null;

    const currentUserIsOwner = Number(state.currentPlayer?.seatNo) === 1;
    const waiting = game.status === "WAITING";

    elements.startGameButton.classList.toggle("hidden", !(currentUserIsOwner && waiting));
    elements.startGameButton.disabled = !waiting;

    if (currentUserIsOwner) {
        elements.leaveRoomButton.textContent = "解散房间";
        elements.actionHint.textContent = waiting
            ? "你是房主。只有你可以开始或解散房间。"
            : "";
    } else {
        elements.leaveRoomButton.textContent = "离开房间";
        elements.actionHint.textContent = waiting
            ? "等待房主开始游戏。玩家变化会自动刷新。"
            : "";
    }

    elements.leaveRoomButton.disabled = !waiting || !state.currentPlayer;
}

function handleRoomState(game) {
    if (state.redirecting) return;

    if (game.status === "RUNNING") {
        state.redirecting = true;
        stopPolling();
        location.replace(gameUrl());
        return;
    }

    if (game.status === "ABORTED") {
        state.redirecting = true;
        stopPolling();

        showMessage(
            "房间已解散",
            "房主已经解散这个房间，即将返回主页。",
            () => location.replace("/home.html")
        );

        setTimeout(() => location.replace("/home.html"), 2500);
        return;
    }

    if (game.status === "FINISHED") {
        state.redirecting = true;
        stopPolling();
        showMessage(
            "比赛已结束",
            "该比赛已经结束，即将返回主页。",
            () => location.replace("/home.html")
        );
        setTimeout(() => location.replace("/home.html"), 2500);
    }
}

function renderRoom(roomAndPlayers) {
    const game = roomAndPlayers?.game;
    const players = Array.isArray(roomAndPlayers?.playerlist)
        ? roomAndPlayers.playerlist
        : [];

    if (!game) throw new Error("房间返回内容中缺少 game");

    state.roomAndPlayers = roomAndPlayers;

    const activePlayers = players.filter(player => player.status === "ACTIVE");
    const owner = players.find(player => Number(player.seatNo) === 1);

    elements.gameIdText.textContent = game.id;
    elements.playerCountText.textContent = `当前 ${activePlayers.length} 名玩家`;
    elements.roomStatusText.textContent = statusText(game.status);
    elements.ownerText.textContent = owner ? playerDisplayName(owner) : "-";
    elements.currentPlayersText.textContent = activePlayers.length;
    elements.currentRoundText.textContent = game.currentRound ?? "-";
    elements.maxRoundText.textContent = game.maxRound ?? "-";

    elements.roomDescription.textContent = {
        WAITING: "等待其他玩家加入，房间成员会自动刷新",
        RUNNING: "比赛已经开始，正在进入游戏页面",
        FINISHED: "比赛已经结束",
        ABORTED: "房间已经被房主解散"
    }[game.status] || "房间状态未知";

    updateStatusBadge(game.status);
    renderPlayers(players);
    renderActions(game, players);
    handleRoomState(game);
}

async function loadCurrentUser() {
    const result = await request(API.currentUser);
    state.userId = Number(result.userId);
    elements.nicknameText.textContent = result.nickname || result.username || "玩家";
}

async function loadRoom(showError = true) {
    if (state.loading || state.redirecting) return;

    state.loading = true;
    elements.refreshButton.disabled = true;

    try {
        const result = await request(API.room(state.gameId));
        if (!result.roomAndPlayers) {
            throw new Error("服务器没有返回房间信息");
        }
        renderRoom(result.roomAndPlayers);
    } catch (error) {
        if (error.status === 401) {
            location.replace("/");
            return;
        }

        if (error.status === 403 || error.status === 404) {
            stopPolling();
            showMessage("无法进入房间", error.message, () => location.replace("/home.html"));
            return;
        }

        if (showError) showMessage("加载房间失败", error.message);
    } finally {
        state.loading = false;
        elements.refreshButton.disabled = false;
    }
}

async function startGame() {
    elements.startGameButton.disabled = true;

    try {
        const result = await request(API.start(state.gameId), {method: "POST"});
        location.replace(result.redirectUrl || gameUrl());
    } catch (error) {
        showMessage("开始游戏失败", error.message);
        elements.startGameButton.disabled = false;
    }
}

async function leaveOrAbortRoom() {
    const owner = Number(state.currentPlayer?.seatNo) === 1;
    const actionName = owner ? "解散房间" : "离开房间";

    if (!window.confirm(`确定要${actionName}吗？`)) return;

    elements.leaveRoomButton.disabled = true;

    try {
        const result = await request(
            owner ? API.abort(state.gameId) : API.leave(state.gameId),
            {method: "POST"}
        );
        location.replace(result.redirectUrl || "/home.html");
    } catch (error) {
        showMessage(`${actionName}失败`, error.message);
        elements.leaveRoomButton.disabled = false;
    }
}

async function logout() {
    elements.logoutButton.disabled = true;

    try {
        await request(API.logout, {method: "POST"});
        location.replace("/");
    } catch (error) {
        showMessage("退出登录失败", error.message);
        elements.logoutButton.disabled = false;
    }
}

async function initialize() {
    state.gameId = getGameIdFromUrl();
    if (!state.gameId) {
        location.replace("/home.html");
        return;
    }

    try {
        await loadCurrentUser();
        document.body.hidden = false;
        await loadRoom(true);

        state.pollingTimer = setInterval(
            () => loadRoom(false),
            2000
        );
    } catch {
        location.replace("/");
    }
}

elements.homeButton.addEventListener("click", () => location.href = "/home.html");
elements.logoutButton.addEventListener("click", logout);
elements.refreshButton.addEventListener("click", () => loadRoom(true));
elements.startGameButton.addEventListener("click", startGame);
elements.leaveRoomButton.addEventListener("click", leaveOrAbortRoom);
elements.closeMessageButton.addEventListener("click", closeMessage);

window.addEventListener("beforeunload", stopPolling);

initialize();
