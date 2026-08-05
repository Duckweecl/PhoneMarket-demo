"use strict";

const API = {
    currentUser: "/api/auth/me",
    logout: "/api/auth/logout",
    updateUsername: "/api/auth/username",
    activeGames: "/api/games/mine/active",
    createGame: "/api/games/create",
    joinGame: gameId => `/api/games/${gameId}/join`
};

const state = {
    userId: null,
    username: null,
    nickname: null,
    activeGames: [],
    activeGamesExpanded: false,
    activeGamesTimer: null
};

const elements = Object.fromEntries([
    "nicknameText",
    "usernameText",
    "welcomeText",
    "changeUsernameButton",
    "logoutButton",
    "refreshActiveGamesButton",
    "activeGameList",
    "activeGameEmpty",
    "toggleActiveGamesButton",
    "createGameButton",
    "joinGameButton",
    "historyButton",
    "watchGameButton",
    "joinModal",
    "joinGameForm",
    "joinGameId",
    "joinInputMessage",
    "closeJoinModalButton",
    "cancelJoinButton",
    "confirmJoinButton",
    "usernameModal",
    "usernameForm",
    "newUsername",
    "usernameInputMessage",
    "closeUsernameModalButton",
    "cancelUsernameButton",
    "confirmUsernameButton",
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

function showMessage(title, text) {
    elements.messageTitle.textContent = title;
    elements.messageText.textContent = text;
    elements.messageModal.classList.remove("hidden");
}

function closeMessage() {
    elements.messageModal.classList.add("hidden");
}

function openJoinModal() {
    elements.joinGameId.value = "";
    elements.joinInputMessage.textContent = "";
    elements.joinModal.classList.remove("hidden");
    elements.joinGameId.focus();
}

function closeJoinModal() {
    elements.joinModal.classList.add("hidden");
}

function openUsernameModal() {
    /*
     * 这里现在修改的是昵称，
     * 所以输入框预设当前昵称。
     */
    elements.newUsername.value =
        state.nickname || "";

    elements.usernameInputMessage.textContent = "";
    elements.usernameModal.classList.remove("hidden");

    elements.newUsername.focus();
    elements.newUsername.select();
}
function closeUsernameModal() {
    elements.usernameModal.classList.add("hidden");
}

function handleUnauthorized(error) {
    if (error.status === 401) {
        location.replace("/");
        return true;
    }
    return false;
}

function roomUrl(gameId) {
    return `/room.html?gameId=${encodeURIComponent(gameId)}`;
}

function gameUrl(gameId) {
    return `/game.html?gameId=${encodeURIComponent(gameId)}`;
}

function getGameId(result) {
    const gameId = Number(result?.roomAndPlayers?.game?.id);
    if (!Number.isInteger(gameId) || gameId <= 0) {
        throw new Error("服务器没有返回有效的房间号");
    }
    return gameId;
}

async function loadCurrentUser() {
    const result = await request(API.currentUser);

    state.userId = Number(result.userId);
    state.username = result.username;
    state.nickname = result.nickname;

    const displayName = result.nickname || result.username || "玩家";
    elements.nicknameText.textContent = displayName;
    elements.usernameText.textContent = `@${result.username}`;
    elements.welcomeText.textContent = `欢迎，${displayName}`;
}

function statusText(status) {
    return {
        WAITING: "等待开始",
        RUNNING: "进行中"
    }[status] || status || "未知状态";
}

function createActiveGameCard(game) {
    const card = document.createElement("article");
    card.className = "active-game-card";

    const main = document.createElement("div");
    main.className = "active-game-main";

    const titleRow = document.createElement("div");
    titleRow.className = "active-game-title-row";

    const title = document.createElement("strong");
    title.textContent = `比赛 #${game.gameId}`;
    titleRow.appendChild(title);

    const status = document.createElement("span");
    status.className = `status-badge ${game.status === "RUNNING" ? "running" : "waiting"}`;
    status.textContent = statusText(game.status);
    titleRow.appendChild(status);

    if (game.owner) {
        const owner = document.createElement("span");
        owner.className = "owner-badge";
        owner.textContent = "房主";
        titleRow.appendChild(owner);
    }

    const meta = document.createElement("div");
    meta.className = "active-game-meta";
    meta.innerHTML = `
        <span>座位 ${escapeHtml(game.seatNo ?? "-")}</span>
        <span>玩家 ${escapeHtml(game.playerCount ?? 0)} / 4</span>
        <span>回合 ${escapeHtml(game.currentRound ?? 1)} / ${escapeHtml(game.maxRound ?? 10)}</span>
    `;

    main.appendChild(titleRow);
    main.appendChild(meta);

    const enterButton = document.createElement("button");
    enterButton.className = "primary-button";
    enterButton.type = "button";
    enterButton.textContent = game.status === "RUNNING" ? "重新进入游戏" : "重新进入房间";
    enterButton.addEventListener("click", () => {
        location.href = game.status === "RUNNING"
            ? gameUrl(game.gameId)
            : roomUrl(game.gameId);
    });

    card.appendChild(main);
    card.appendChild(enterButton);
    return card;
}

function renderActiveGames() {
    elements.activeGameList.replaceChildren();

    const games = state.activeGames || [];
    elements.activeGameEmpty.classList.toggle("hidden", games.length > 0);

    const visibleGames = state.activeGamesExpanded
        ? games
        : games.slice(0, 3);

    for (const game of visibleGames) {
        elements.activeGameList.appendChild(createActiveGameCard(game));
    }

    const hasMore = games.length > 3;
    elements.toggleActiveGamesButton.classList.toggle("hidden", !hasMore);
    elements.toggleActiveGamesButton.textContent = state.activeGamesExpanded
        ? "收起"
        : `展开另外 ${games.length - 3} 场比赛`;
}

async function loadActiveGames(showError = false) {
    elements.refreshActiveGamesButton.disabled = true;

    try {
        const result = await request(API.activeGames);
        state.activeGames = Array.isArray(result.games) ? result.games : [];
        renderActiveGames();
    } catch (error) {
        if (handleUnauthorized(error)) return;
        if (showError) showMessage("读取比赛失败", error.message);
    } finally {
        elements.refreshActiveGamesButton.disabled = false;
    }
}

async function createGame() {
    elements.createGameButton.disabled = true;

    try {
        const result = await request(API.createGame, {method: "POST"});
        const gameId = getGameId(result);
        location.href = result.redirectUrl || roomUrl(gameId);
    } catch (error) {
        if (!handleUnauthorized(error)) {
            showMessage("创建房间失败", error.message);
        }
    } finally {
        elements.createGameButton.disabled = false;
    }
}

async function joinGame(gameId) {
    elements.confirmJoinButton.disabled = true;
    closeJoinModal();

    try {
        const result = await request(API.joinGame(gameId), {method: "POST"});
        const returnedGameId = getGameId(result);
        location.href = result.redirectUrl || roomUrl(returnedGameId);
    } catch (error) {
        if (!handleUnauthorized(error)) {
            showMessage("加入房间失败", error.message);
        }
    } finally {
        elements.confirmJoinButton.disabled = false;
    }
}

async function updateUsername(nickname) {
    elements.confirmUsernameButton.disabled = true;

    const newNickname = nickname.trim();

    try {
        const result = await request(
            API.updateUsername,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    /*
                     * 如果后端接收字段仍叫 username，
                     * 暂时保持这个字段名。
                     */
                    username: newNickname
                })
            }
        );

        /*
         * 不依赖 result.username。
         * 直接使用用户刚刚输入的新昵称。
         */
        state.nickname = newNickname;

        elements.nicknameText.textContent =
            newNickname;

        if (elements.welcomeText) {
            elements.welcomeText.textContent =
                `欢迎，${newNickname}`;
        }

        /*
         * 不修改 usernameText。
         * @ 后面继续显示原登录用户名。
         */

        closeUsernameModal();

        showMessage(
            "修改成功",
            result.message || "昵称修改成功"
        );

    } catch (error) {
        if (handleUnauthorized(error)) {
            return;
        }

        elements.usernameInputMessage.textContent =
            error.message || "昵称修改失败";

    } finally {
        elements.confirmUsernameButton.disabled = false;
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
    try {
        await loadCurrentUser();
        document.body.hidden = false;
        await loadActiveGames(true);

        state.activeGamesTimer = setInterval(
            () => loadActiveGames(false),
            5000
        );
    } catch (error) {
        location.replace("/");
    }
}

elements.createGameButton.addEventListener("click", createGame);
elements.joinGameButton.addEventListener("click", openJoinModal);
elements.changeUsernameButton.addEventListener("click", openUsernameModal);
elements.logoutButton.addEventListener("click", logout);
elements.refreshActiveGamesButton.addEventListener("click", () => loadActiveGames(true));

elements.toggleActiveGamesButton.addEventListener("click", () => {
    state.activeGamesExpanded = !state.activeGamesExpanded;
    renderActiveGames();
});

elements.joinGameForm.addEventListener("submit", event => {
    event.preventDefault();
    const gameId = Number(elements.joinGameId.value);

    if (!Number.isInteger(gameId) || gameId <= 0) {
        elements.joinInputMessage.textContent = "请输入有效的房间号";
        return;
    }

    elements.joinInputMessage.textContent = "";
    joinGame(gameId);
});

elements.usernameForm.addEventListener("submit", event => {
    event.preventDefault();
    const username = elements.newUsername.value.trim();


    elements.usernameInputMessage.textContent = "";
    updateUsername(username);
});

elements.historyButton.addEventListener("click", () => {
    showMessage("比赛记录", "比赛记录功能暂未实现。接口位置已经保留。 ");
});

elements.watchGameButton.addEventListener("click", () => {
    showMessage("观看比赛", "观看比赛功能暂未实现。接口位置已经保留。 ");
});

[
    [elements.closeJoinModalButton, closeJoinModal],
    [elements.cancelJoinButton, closeJoinModal],
    [elements.closeUsernameModalButton, closeUsernameModal],
    [elements.cancelUsernameButton, closeUsernameModal],
    [elements.closeMessageButton, closeMessage]
].forEach(([button, handler]) => button.addEventListener("click", handler));

[elements.joinModal, elements.usernameModal, elements.messageModal]
    .forEach(modal => modal.addEventListener("click", event => {
        if (event.target !== modal) return;
        modal.classList.add("hidden");
    }));

document.addEventListener("keydown", event => {
    if (event.key !== "Escape") return;
    closeJoinModal();
    closeUsernameModal();
    closeMessage();
});

window.addEventListener("beforeunload", () => {
    if (state.activeGamesTimer) clearInterval(state.activeGamesTimer);
});

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

initialize();
