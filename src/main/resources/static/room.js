"use strict";


const API = {
    currentUser: "/api/auth/me",
    logout: "/api/auth/logout",

    roomDetail: gameId =>
        `/api/games/${gameId}/room`,

    startGame: gameId =>
        `/api/games/${gameId}/start`,

    leaveRoom: gameId =>
        `/api/games/${gameId}/leave`,

    abortRoom: gameId =>
        `/api/games/${gameId}/abort`
};


const state = {
    userId: null,
    username: null,
    nickname: null,

    gameId: null,

    roomAndPlayers: null,

    currentPlayer: null,
    ownerPlayer: null,

    refreshTimer: null,
    loadingRoom: false
};


const elements = {
    nicknameText:
        document.getElementById("nicknameText"),

    homeButton:
        document.getElementById("homeButton"),

    logoutButton:
        document.getElementById("logoutButton"),

    gameIdText:
        document.getElementById("gameIdText"),

    roomDescription:
        document.getElementById("roomDescription"),

    statusBadge:
        document.getElementById("statusBadge"),

    playerCountText:
        document.getElementById("playerCountText"),

    playerList:
        document.getElementById("playerList"),

    emptyPlayerText:
        document.getElementById("emptyPlayerText"),

    roomStatusText:
        document.getElementById("roomStatusText"),

    ownerText:
        document.getElementById("ownerText"),

    currentPlayersText:
        document.getElementById("currentPlayersText"),

    currentRoundText:
        document.getElementById("currentRoundText"),

    maxRoundText:
        document.getElementById("maxRoundText"),

    refreshButton:
        document.getElementById("refreshButton"),

    startGameButton:
        document.getElementById("startGameButton"),

    leaveRoomButton:
        document.getElementById("leaveRoomButton"),

    actionHint:
        document.getElementById("actionHint"),

    messageModal:
        document.getElementById("messageModal"),

    messageTitle:
        document.getElementById("messageTitle"),

    messageText:
        document.getElementById("messageText"),

    closeMessageButton:
        document.getElementById("closeMessageButton")
};


async function request(url, options = {}) {
    const {
        headers = {},
        ...otherOptions
    } = options;

    const response = await fetch(url, {
        credentials: "same-origin",

        ...otherOptions,

        headers: {
            Accept: "application/json",
            ...headers
        }
    });

    const responseText = await response.text();

    let result = {};

    if (responseText) {
        try {
            result = JSON.parse(responseText);
        } catch (error) {
            result = {
                message: responseText
            };
        }
    }

    if (!response.ok || result.success === false) {
        const requestError = new Error(
            result.message ||
            result.detail ||
            result.error ||
            `请求失败，状态码：${response.status}`
        );

        requestError.status = response.status;

        throw requestError;
    }

    return result;
}


function showMessage(title, text) {
    elements.messageTitle.textContent = title;
    elements.messageText.textContent = text;

    elements.messageModal.classList.remove("hidden");
}


function closeMessage() {
    elements.messageModal.classList.add("hidden");
}


function getGameIdFromUrl() {
    const parameters =
        new URLSearchParams(location.search);

    const gameId =
        Number(parameters.get("gameId"));

    if (!Number.isInteger(gameId) || gameId <= 0) {
        return null;
    }

    return gameId;
}


function getGameStatusText(status) {
    const statusMap = {
        WAITING: "等待开始",
        RUNNING: "比赛进行中",
        IN_PROGRESS: "比赛进行中",
        FINISHED: "比赛已结束",
        ABORTED: "房间已解散"
    };

    return statusMap[status] ||
        status ||
        "未知状态";
}


function getPlayerStatusText(status) {
    const statusMap = {
        ACTIVE: "房间中",
        WAITING: "等待中",
        LEFT: "已离开",
        FINISHED: "已完成"
    };

    return statusMap[status] ||
        status ||
        "房间中";
}


function updateStatusBadge(status) {
    elements.statusBadge.className =
        "status-badge";

    if (status === "WAITING") {
        elements.statusBadge.classList.add(
            "waiting"
        );

    } else if (
        status === "RUNNING" ||
        status === "IN_PROGRESS"
    ) {
        elements.statusBadge.classList.add(
            "running"
        );

    } else if (status === "FINISHED") {
        elements.statusBadge.classList.add(
            "finished"
        );

    } else {
        elements.statusBadge.classList.add(
            "aborted"
        );
    }

    elements.statusBadge.textContent =
        getGameStatusText(status);
}


function createLabel(text, className) {
    const label =
        document.createElement("span");

    label.className = className;
    label.textContent = text;

    return label;
}


function createPlayerCard(player) {
    const card =
        document.createElement("div");

    card.className = "player-card";

    const main =
        document.createElement("div");

    main.className = "player-main";

    const avatar =
        document.createElement("div");

    avatar.className = "player-avatar";
    avatar.textContent =
        player.seatNo ?? "?";

    const information =
        document.createElement("div");

    const name =
        document.createElement("div");

    name.className = "player-name";
    name.textContent =
        `玩家 ${player.userId}`;

    const extra =
        document.createElement("div");

    extra.className = "player-extra";
    extra.textContent =
        `座位 ${player.seatNo ?? "-"}`;

    information.appendChild(name);
    information.appendChild(extra);

    main.appendChild(avatar);
    main.appendChild(information);

    const labels =
        document.createElement("div");

    labels.className = "player-labels";

    if (Number(player.seatNo) === 1) {
        labels.appendChild(
            createLabel(
                "房主",
                "owner-label"
            )
        );
    }

    if (
        state.userId !== null &&
        Number(player.userId) === state.userId
    ) {
        labels.appendChild(
            createLabel(
                "你",
                "current-user-label"
            )
        );
    }

    labels.appendChild(
        createLabel(
            getPlayerStatusText(player.status),
            "player-status-label"
        )
    );

    card.appendChild(main);
    card.appendChild(labels);

    return card;
}


function renderPlayers(playerlist) {
    elements.playerList.replaceChildren();

    if (
        !Array.isArray(playerlist) ||
        playerlist.length === 0
    ) {
        elements.emptyPlayerText.classList.remove(
            "hidden"
        );

        return;
    }

    elements.emptyPlayerText.classList.add(
        "hidden"
    );

    const sortedPlayers = [...playerlist].sort(
        function (firstPlayer, secondPlayer) {
            return (
                Number(firstPlayer.seatNo) -
                Number(secondPlayer.seatNo)
            );
        }
    );

    for (const player of sortedPlayers) {
        elements.playerList.appendChild(
            createPlayerCard(player)
        );
    }
}


function updateRoomActions(game, playerlist) {
    state.currentPlayer =
        playerlist.find(
            player =>
                Number(player.userId) === state.userId
        ) || null;

    state.ownerPlayer =
        playerlist.find(
            player =>
                Number(player.seatNo) === 1
        ) || null;

    const currentUserIsOwner =
        state.currentPlayer !== null &&
        Number(state.currentPlayer.seatNo) === 1;

    const roomIsWaiting =
        game.status === "WAITING";

    /*
     * 只有房主并且房间为 WAITING 时显示开始按钮。
     */
    if (currentUserIsOwner && roomIsWaiting) {
        elements.startGameButton.classList.remove(
            "hidden"
        );

        elements.startGameButton.disabled = false;

        elements.actionHint.textContent =
            "你是房主，可以开始比赛。";

    } else {
        elements.startGameButton.classList.add(
            "hidden"
        );

        elements.actionHint.textContent =
            roomIsWaiting
                ? "等待房主开始比赛。"
                : "";
    }

    /*
     * 房主点击时解散房间，
     * 普通玩家点击时离开房间。
     */
    if (currentUserIsOwner) {
        elements.leaveRoomButton.textContent =
            "解散房间";
    } else {
        elements.leaveRoomButton.textContent =
            "离开房间";
    }

    elements.leaveRoomButton.disabled =
        !state.currentPlayer;
}


function renderRoom(roomAndPlayers) {
    const game =
        roomAndPlayers.game;

    const playerlist =
        Array.isArray(roomAndPlayers.playerlist)
            ? roomAndPlayers.playerlist
            : [];

    if (!game) {
        throw new Error(
            "房间信息中缺少 game"
        );
    }

    state.roomAndPlayers = roomAndPlayers;

    const playerCount =
        game.playerCount ?? playerlist.length;

    elements.gameIdText.textContent =
        game.id;

    elements.playerCountText.textContent =
        `当前 ${playerCount} 名玩家`;

    elements.roomStatusText.textContent =
        getGameStatusText(game.status);

    elements.currentPlayersText.textContent =
        playerCount;

    elements.currentRoundText.textContent =
        game.currentRound ?? "-";

    elements.maxRoundText.textContent =
        game.maxRound ?? "-";

    const owner =
        playerlist.find(
            player =>
                Number(player.seatNo) === 1
        );

    elements.ownerText.textContent =
        owner
            ? `玩家 ${owner.userId}`
            : "-";

    if (game.status === "WAITING") {
        elements.roomDescription.textContent =
            "等待其他玩家加入房间";

    } else if (
        game.status === "RUNNING" ||
        game.status === "IN_PROGRESS"
    ) {
        elements.roomDescription.textContent =
            "比赛正在进行中";

    } else if (game.status === "FINISHED") {
        elements.roomDescription.textContent =
            "比赛已经结束";

    } else {
        elements.roomDescription.textContent =
            "房间已经解散";
    }

    updateStatusBadge(game.status);
    renderPlayers(playerlist);
    updateRoomActions(game, playerlist);
}


async function loadCurrentUser() {
    const result = await request(
        API.currentUser
    );

    if (!result.success) {
        throw new Error("用户未登录");
    }

    state.userId = result.userId
        ? Number(result.userId)
        : null;

    state.username = result.username;
    state.nickname = result.nickname;

    elements.nicknameText.textContent =
        result.nickname ||
        result.username ||
        "玩家";
}


/**
 * 查询房间。
 *
 * 后端返回：
 * result.roomAndPlayers.game
 * result.roomAndPlayers.playerlist
 */
async function loadRoom(showError = true) {
    if (state.loadingRoom) {
        return false;
    }

    state.loadingRoom = true;
    elements.refreshButton.disabled = true;

    try {
        const result = await request(
            API.roomDetail(state.gameId)
        );

        if (!result.roomAndPlayers) {
            throw new Error(
                result.message ||
                "服务器没有返回 roomAndPlayers"
            );
        }

        renderRoom(result.roomAndPlayers);

        return true;

    } catch (error) {
        console.error(
            "加载房间失败：",
            error
        );

        if (error.status === 401) {
            location.replace("/");
            return false;
        }

        if (showError) {
            showMessage(
                "加载房间失败",
                error.message
            );
        }

        return false;

    } finally {
        state.loadingRoom = false;
        elements.refreshButton.disabled = false;
    }
}


async function startGame() {
    elements.startGameButton.disabled = true;

    try {
        const result = await request(
            API.startGame(state.gameId),
            {
                method: "POST"
            }
        );

        showMessage(
            "开始比赛",
            result.message || "比赛开始成功"
        );

        await loadRoom(false);

        /*
         * 游戏页面完成后，可以在这里跳转：
         *
         * location.href =
         *     `/game.html?gameId=${state.gameId}`;
         */

    } catch (error) {
        showMessage(
            "开始比赛失败",
            error.message
        );

    } finally {
        elements.startGameButton.disabled = false;
    }
}


async function leaveOrAbortRoom() {
    if (!state.currentPlayer) {
        showMessage(
            "操作失败",
            "当前用户不在这个房间中"
        );

        return;
    }

    const currentUserIsOwner =
        Number(state.currentPlayer.seatNo) === 1;

    const actionName =
        currentUserIsOwner
            ? "解散房间"
            : "离开房间";

    const confirmed = window.confirm(
        `确定要${actionName}吗？`
    );

    if (!confirmed) {
        return;
    }

    elements.leaveRoomButton.disabled = true;

    try {
        const url = currentUserIsOwner
            ? API.abortRoom(state.gameId)
            : API.leaveRoom(state.gameId);

        await request(
            url,
            {
                method: "POST"
            }
        );

        location.replace("/home.html");

    } catch (error) {
        showMessage(
            `${actionName}失败`,
            error.message
        );

        elements.leaveRoomButton.disabled = false;
    }
}


async function logout() {
    elements.logoutButton.disabled = true;

    try {
        await request(
            API.logout,
            {
                method: "POST"
            }
        );

        location.replace("/");

    } catch (error) {
        showMessage(
            "退出登录失败",
            error.message
        );

        elements.logoutButton.disabled = false;
    }
}


async function initializePage() {
    state.gameId = getGameIdFromUrl();

    if (!state.gameId) {
        location.replace("/home.html");
        return;
    }

    try {
        await loadCurrentUser();

    } catch (error) {
        console.error(error);
        location.replace("/");
        return;
    }

    document.body.hidden = false;

    const loaded =
        await loadRoom(true);

    if (loaded) {
        /*
         * 每三秒查询一次最新房间信息。
         */
        state.refreshTimer = setInterval(
            function () {
                loadRoom(false);
            },
            3000
        );
    }
}


elements.homeButton.addEventListener(
    "click",
    function () {
        location.href = "/home.html";
    }
);


elements.logoutButton.addEventListener(
    "click",
    logout
);


elements.refreshButton.addEventListener(
    "click",
    function () {
        loadRoom(true);
    }
);


elements.startGameButton.addEventListener(
    "click",
    startGame
);


elements.leaveRoomButton.addEventListener(
    "click",
    leaveOrAbortRoom
);


elements.closeMessageButton.addEventListener(
    "click",
    closeMessage
);


elements.messageModal.addEventListener(
    "click",
    function (event) {
        if (event.target === elements.messageModal) {
            closeMessage();
        }
    }
);


window.addEventListener(
    "beforeunload",
    function () {
        if (state.refreshTimer) {
            clearInterval(state.refreshTimer);
        }
    }
);


initializePage();