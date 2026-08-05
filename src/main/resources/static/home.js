"use strict";


const API = {
    currentUser: "/api/auth/me",
    logout: "/api/auth/logout",

    createGame: "/api/games/create",

    joinGame: gameId =>
        `/api/games/${gameId}/join`,

    /*
     * 以下功能暂时没有实际接口。
     */
    activeGames: "/api/games/mine/active",
    updateNickname: "/api/auth/nickname",
    history: "/api/games/mine/history",
    watchableGames: "/api/games/watchable"
};


const state = {
    userId: null,
    username: null,
    nickname: null
};


const elements = {
    nicknameText:
        document.getElementById("nicknameText"),

    welcomeText:
        document.getElementById("welcomeText"),

    changeNicknameButton:
        document.getElementById("changeNicknameButton"),

    logoutButton:
        document.getElementById("logoutButton"),

    createGameButton:
        document.getElementById("createGameButton"),

    joinGameButton:
        document.getElementById("joinGameButton"),

    historyButton:
        document.getElementById("historyButton"),

    watchGameButton:
        document.getElementById("watchGameButton"),

    joinModal:
        document.getElementById("joinModal"),

    joinGameForm:
        document.getElementById("joinGameForm"),

    joinGameId:
        document.getElementById("joinGameId"),

    joinInputMessage:
        document.getElementById("joinInputMessage"),

    closeJoinModalButton:
        document.getElementById("closeJoinModalButton"),

    cancelJoinButton:
        document.getElementById("cancelJoinButton"),

    confirmJoinButton:
        document.getElementById("confirmJoinButton"),

    messageModal:
        document.getElementById("messageModal"),

    messageTitle:
        document.getElementById("messageTitle"),

    messageText:
        document.getElementById("messageText"),

    closeMessageButton:
        document.getElementById("closeMessageButton")
};


/**
 * 统一发送请求。
 */
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


function openJoinModal() {
    elements.joinGameId.value = "";
    elements.joinInputMessage.textContent = "";

    elements.joinModal.classList.remove("hidden");

    elements.joinGameId.focus();
}


function closeJoinModal() {
    elements.joinModal.classList.add("hidden");
}


/**
 * RoomResultResponse 中：
 *
 * result.roomAndPlayers.game.id
 */
function getGameId(result) {
    const gameId = Number(
        result?.roomAndPlayers?.game?.id
    );

    if (!Number.isInteger(gameId) || gameId <= 0) {
        throw new Error(
            result?.message ||
            "服务器没有返回有效的房间号"
        );
    }

    return gameId;
}


function goToRoom(gameId) {
    location.href =
        `/room.html?gameId=${encodeURIComponent(gameId)}`;
}


/**
 * 检查登录并显示当前用户。
 */
async function loadCurrentUser() {
    try {
        const result = await request(
            API.currentUser
        );

        if (!result.success) {
            location.replace("/");
            return;
        }

        state.userId = result.userId
            ? Number(result.userId)
            : null;

        state.username = result.username;
        state.nickname = result.nickname;

        const displayName =
            result.nickname ||
            result.username ||
            "玩家";

        elements.nicknameText.textContent =
            displayName;

        elements.welcomeText.textContent =
            `欢迎，${displayName}`;

        document.body.hidden = false;

    } catch (error) {
        console.error(
            "读取当前用户失败：",
            error
        );

        location.replace("/");
    }
}


/**
 * 创建房间。
 *
 * 后端从 Session 取得 userId，
 * 前端不发送 userId。
 */
async function createGame() {
    elements.createGameButton.disabled = true;

    try {
        const result = await request(
            API.createGame,
            {
                method: "POST"
            }
        );

        const gameId = getGameId(result);

        goToRoom(gameId);

    } catch (error) {
        if (error.status === 401) {
            location.replace("/");
            return;
        }

        showMessage(
            "创建房间失败",
            error.message
        );

    } finally {
        elements.createGameButton.disabled = false;
    }
}


/**
 * 加入房间。
 */
async function joinGame(gameId) {
    elements.joinGameButton.disabled = true;
    elements.confirmJoinButton.disabled = true;

    /*
     * 用户确认加入后先关闭输入弹窗。
     */
    closeJoinModal();

    try {
        const result = await request(
            API.joinGame(gameId),
            {
                method: "POST"
            }
        );

        const returnedGameId = getGameId(result);

        goToRoom(returnedGameId);

    } catch (error) {
        if (error.status === 401) {
            location.replace("/");
            return;
        }

        showMessage(
            "加入房间失败",
            error.message
        );

    } finally {
        elements.joinGameButton.disabled = false;
        elements.confirmJoinButton.disabled = false;
    }
}


/**
 * 退出登录。
 */
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


elements.createGameButton.addEventListener(
    "click",
    createGame
);


elements.joinGameButton.addEventListener(
    "click",
    openJoinModal
);


elements.closeJoinModalButton.addEventListener(
    "click",
    closeJoinModal
);


elements.cancelJoinButton.addEventListener(
    "click",
    closeJoinModal
);


elements.joinGameForm.addEventListener(
    "submit",
    function (event) {
        event.preventDefault();

        const gameId =
            Number(elements.joinGameId.value);

        if (!Number.isInteger(gameId)
            || gameId <= 0) {
            elements.joinInputMessage.textContent =
                "请输入有效的房间号";

            return;
        }

        elements.joinInputMessage.textContent = "";

        joinGame(gameId);
    }
);


elements.changeNicknameButton.addEventListener(
    "click",
    function () {
        showMessage(
            "修改昵称",
            "修改昵称功能暂未实现。\n预留接口：PUT /api/auth/nickname"
        );
    }
);


elements.historyButton.addEventListener(
    "click",
    function () {
        showMessage(
            "比赛记录",
            "比赛记录功能暂未实现。\n预留接口：GET /api/games/mine/history"
        );
    }
);


elements.watchGameButton.addEventListener(
    "click",
    function () {
        showMessage(
            "观看比赛",
            "观看比赛功能暂未实现。\n预留接口：GET /api/games/watchable"
        );
    }
);


elements.logoutButton.addEventListener(
    "click",
    logout
);


elements.closeMessageButton.addEventListener(
    "click",
    closeMessage
);


/**
 * 点击弹窗背景时关闭。
 */
elements.joinModal.addEventListener(
    "click",
    function (event) {
        if (event.target === elements.joinModal) {
            closeJoinModal();
        }
    }
);


elements.messageModal.addEventListener(
    "click",
    function (event) {
        if (event.target === elements.messageModal) {
            closeMessage();
        }
    }
);


document.addEventListener(
    "keydown",
    function (event) {
        if (event.key !== "Escape") {
            return;
        }

        closeJoinModal();
        closeMessage();
    }
);


loadCurrentUser();