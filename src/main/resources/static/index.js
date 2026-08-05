"use strict";

const loginPage = document.getElementById("loginPage");
const registerPage = document.getElementById("registerPage");
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const goToRegisterButton = document.getElementById("goToRegisterButton");
const backToLoginButton = document.getElementById("backToLoginButton");
const message = document.getElementById("message");

function showLoginPage() {
    loginPage.classList.remove("hidden");
    registerPage.classList.add("hidden");
    message.textContent = "";
}

function showRegisterPage() {
    loginPage.classList.add("hidden");
    registerPage.classList.remove("hidden");
    message.textContent = "";
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options
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
        throw new Error(body.message || `请求失败：${response.status}`);
    }

    return body;
}

goToRegisterButton.addEventListener("click", showRegisterPage);
backToLoginButton.addEventListener("click", showLoginPage);

loginForm.addEventListener("submit", async event => {
    event.preventDefault();

    const username = document.getElementById("loginUsername").value.trim();
    const passwordInput = document.getElementById("loginPassword");

    try {
        message.textContent = "正在登录……";

        const result = await request("/api/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                username,
                password: passwordInput.value
            })
        });

        message.textContent = result.message;
        location.replace("/home.html");
    } catch (error) {
        passwordInput.value = "";
        message.textContent = error.message;
    }
});

registerForm.addEventListener("submit", async event => {
    event.preventDefault();

    const nickname = document.getElementById("registerNickname").value.trim();
    const username = document.getElementById("registerUsername").value.trim();
    const passwordInput = document.getElementById("registerPassword");

    try {
        message.textContent = "正在注册……";

        const result = await request("/api/auth/register", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                nickname,
                username,
                password: passwordInput.value
            })
        });

        showLoginPage();
        document.getElementById("loginUsername").value = username;
        document.getElementById("loginPassword").value = "";
        passwordInput.value = "";
        message.textContent = result.message;
    } catch (error) {
        message.textContent = error.message;
    }
});
