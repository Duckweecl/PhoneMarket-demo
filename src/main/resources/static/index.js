const loginPage = document.getElementById("loginPage");
const registerPage = document.getElementById("registerPage");

const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");

const goToRegisterButton =
    document.getElementById("goToRegisterButton");

const backToLoginButton =
    document.getElementById("backToLoginButton");

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


goToRegisterButton.addEventListener(
    "click",
    showRegisterPage
);


backToLoginButton.addEventListener(
    "click",
    showLoginPage
);


loginForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const username =
        document.getElementById("loginUsername").value.trim();

    const password =
        document.getElementById("loginPassword").value;

    try {
        message.textContent = "正在登录……";

        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username,
                password
            })
        });

        const result = await response.json();

        if (!result.success) {
            message.textContent =
                result.message;

            document.getElementById("loginPassword").value = "";
            return;
        }

        message.textContent = result.message;

        location.href = "/home.html";

    } catch (error) {
        console.error(error);
        message.textContent = "无法连接服务器";
    }
});




registerForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const nickname =
        document.getElementById("registerNickname").value.trim();

    const username =
        document.getElementById("registerUsername").value.trim();

    const password =
        document.getElementById("registerPassword").value;

    const registerData = {
        nickname,
        username,
        password
    };

    try {
        message.textContent = "正在注册……";

        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(registerData)
        });

        const result = await response.json();

        if (!response.ok) {
            message.textContent = result.message || "注册失败";
            return;
        }

        message.textContent =
            `${result.message}`;

    } catch (error) {
        console.error(error);
        message.textContent = "无法连接服务器";
    }
});