(() => {
    "use strict";

    const AUTH_TAB_KEY = "pms_auth_tab";
    const FORGOT_SEND_CODE_SECONDS = 60;
    const FORGOT_STEP_ORDER = {
        verify: 1,
        reset: 2,
        success: 3
    };

    function setupAuthPage() {
        const card = document.querySelector(".auth-portal-card");
        if (!card) {
            return;
        }

        const initialTab = card.getAttribute("data-initial-tab") || "login";
        const initialForgotOpen = card.getAttribute("data-forgot-open") === "true";
        const initialForgotStep = card.getAttribute("data-forgot-step") || "verify";
        const tabButtons = document.querySelectorAll(".auth-tab-btn");
        const tabNav = document.querySelector(".auth-portal-tabs");
        const tabUnderline = tabNav ? tabNav.querySelector(".slide-underline") : null;
        const loginSection = document.querySelector(".auth-portal-section-login");
        const registerSection = document.querySelector(".auth-portal-section-register");
        const forgotSection = document.querySelector(".auth-portal-section-forgot");
        const forgotSteps = document.querySelectorAll(".auth-forgot-step");
        const forgotProgressItems = document.querySelectorAll("[data-step-indicator]");
        const quickSwitch = document.querySelectorAll("[data-switch-tab]");
        const forgotForm = document.getElementById("forgotForm");
        const forgotUsernameInput = document.getElementById("forgot-username");
        const forgotCodeInput = document.getElementById("forgot-code");
        const forgotNewPasswordInput = document.getElementById("forgot-new-password");
        const forgotConfirmPasswordInput = document.getElementById("forgot-confirm-password");
        const forgotUsernamePreview = document.getElementById("forgotUsernamePreview");
        const forgotVerifyFeedback = document.getElementById("forgotVerifyFeedback");
        const forgotResetFeedback = document.getElementById("forgotResetFeedback");
        const forgotSendCodeButton = document.getElementById("forgotSendCodeBtn");
        const forgotOpenButtons = document.querySelectorAll("[data-open-forgot-panel]");
        const forgotCloseButtons = document.querySelectorAll("[data-close-forgot-panel]");
        const forgotNextButtons = document.querySelectorAll("[data-forgot-next]");
        const forgotPrevButtons = document.querySelectorAll("[data-forgot-prev]");
        const loginForm = document.getElementById("loginForm");
        const loginUsernameInput = document.getElementById("login-username");
        const loginPasswordInput = document.getElementById("login-password");
        const preserveLoginUsername = loginForm && loginForm.getAttribute("data-preserve-username") === "true";
        const formInputs = document.querySelectorAll(
            "#login-username, #login-password, #register-username, #register-password, #register-confirm-password, #forgot-username, #forgot-code, #forgot-new-password, #forgot-confirm-password"
        );
        const sensitiveInputs = document.querySelectorAll(
            "#login-password, #register-password, #register-confirm-password, #forgot-new-password, #forgot-confirm-password"
        );
        const clearTimerIds = [];
        const runtimeMessageTimers = [];
        let activeTab = initialTab === "register" ? "register" : "login";
        let isForgotPanelOpen = initialForgotOpen;
        let currentForgotStep = normalizeForgotStep(initialForgotStep);
        let lastPanelTrigger = null;
        let forgotCountdownTimerId = null;
        let forgotCountdownValue = FORGOT_SEND_CODE_SECONDS;

        function normalizeForgotStep(step) {
            return FORGOT_STEP_ORDER[step] ? step : "verify";
        }

        function setUnderline(targetBtn) {
            if (!tabUnderline || !targetBtn) return;
            tabUnderline.style.width = `${targetBtn.offsetWidth}px`;
            tabUnderline.style.left = `${targetBtn.offsetLeft}px`;
        }

        function getActiveTabButton() {
            return Array.from(tabButtons).find((btn) => btn.getAttribute("data-tab-target") === activeTab) || null;
        }

        function renderSections() {
            if (loginSection) {
                loginSection.classList.toggle("is-hidden", isForgotPanelOpen || activeTab !== "login");
            }
            if (registerSection) {
                registerSection.classList.toggle("is-hidden", isForgotPanelOpen || activeTab !== "register");
            }
            if (forgotSection) {
                forgotSection.classList.toggle("is-hidden", !isForgotPanelOpen);
                forgotSection.setAttribute("aria-hidden", isForgotPanelOpen ? "false" : "true");
            }
            if (tabNav) {
                tabNav.classList.toggle("is-hidden", isForgotPanelOpen);
            }
            if (card) {
                card.classList.toggle("is-forgot-active", isForgotPanelOpen);
                card.setAttribute("data-forgot-open", isForgotPanelOpen ? "true" : "false");
            }
            tabButtons.forEach((btn) => {
                const isActive = btn.getAttribute("data-tab-target") === activeTab;
                btn.classList.toggle("is-active", isActive);
            });
            if (!isForgotPanelOpen) {
                setUnderline(getActiveTabButton());
            }
        }

        function switchTab(tab) {
            activeTab = tab === "register" ? "register" : "login";
            isForgotPanelOpen = false;
            if (activeTab !== "login") {
                clearRuntimeMessage();
            }
            renderSections();
        }

        function updateForgotUsernamePreview() {
            if (!forgotUsernamePreview || !forgotUsernameInput) {
                return;
            }
            const username = forgotUsernameInput.value.trim();
            forgotUsernamePreview.textContent = username || "未填写用户名";
        }

        function setForgotFeedback(target, message = "") {
            const element = target === "reset" ? forgotResetFeedback : forgotVerifyFeedback;
            if (!element) {
                return;
            }
            const resolvedMessage = (message || "").trim();
            element.textContent = resolvedMessage;
            element.classList.toggle("is-visible", Boolean(resolvedMessage));
        }

        function clearForgotFeedback() {
            setForgotFeedback("verify", "");
            setForgotFeedback("reset", "");
        }

        function renderForgotStep(options = {}) {
            const { focus = true } = options;
            currentForgotStep = normalizeForgotStep(currentForgotStep);

            forgotSteps.forEach((stepElement) => {
                const isActive = stepElement.getAttribute("data-step") === currentForgotStep;
                stepElement.classList.toggle("is-hidden", !isActive);
                stepElement.setAttribute("aria-hidden", isActive ? "false" : "true");
            });

            forgotProgressItems.forEach((stepItem) => {
                const step = normalizeForgotStep(stepItem.getAttribute("data-step-indicator"));
                const stepOrder = FORGOT_STEP_ORDER[step];
                const currentOrder = FORGOT_STEP_ORDER[currentForgotStep];
                stepItem.classList.toggle("is-active", step === currentForgotStep);
                stepItem.classList.toggle("is-complete", stepOrder < currentOrder);
            });

            card.setAttribute("data-forgot-step", currentForgotStep);
            updateForgotUsernamePreview();

            if (!focus) {
                return;
            }

            const targetInput = currentForgotStep === "verify"
                ? forgotUsernameInput
                : currentForgotStep === "reset"
                    ? forgotNewPasswordInput
                    : forgotCloseButtons[forgotCloseButtons.length - 1] || forgotCloseButtons[0];

            if (targetInput && typeof targetInput.focus === "function") {
                window.setTimeout(() => {
                    targetInput.focus();
                    if (typeof targetInput.select === "function" && currentForgotStep === "verify") {
                        targetInput.select();
                    }
                }, 40);
            }
        }

        function syncForgotInputsFromLogin() {
            if (!forgotUsernameInput || !loginUsernameInput) {
                return;
            }
            if (!forgotUsernameInput.value.trim() && loginUsernameInput.value.trim()) {
                forgotUsernameInput.value = loginUsernameInput.value.trim();
                updateForgotUsernamePreview();
            }
        }

        function resetForgotCountdownButton() {
            if (!forgotSendCodeButton) {
                return;
            }
            forgotSendCodeButton.disabled = false;
            forgotSendCodeButton.textContent = "发送验证码";
        }

        function stopForgotCountdown(resetButton = true) {
            if (forgotCountdownTimerId !== null) {
                window.clearInterval(forgotCountdownTimerId);
                forgotCountdownTimerId = null;
            }
            forgotCountdownValue = FORGOT_SEND_CODE_SECONDS;
            if (resetButton) {
                resetForgotCountdownButton();
            }
        }

        function startForgotCountdown() {
            if (!forgotSendCodeButton) {
                return;
            }
            stopForgotCountdown(false);
            forgotSendCodeButton.disabled = true;
            forgotSendCodeButton.textContent = `${forgotCountdownValue}s`;
            forgotCountdownTimerId = window.setInterval(() => {
                forgotCountdownValue -= 1;
                if (forgotCountdownValue <= 0) {
                    stopForgotCountdown(true);
                    return;
                }
                forgotSendCodeButton.textContent = `${forgotCountdownValue}s`;
            }, 1000);
        }

        function resetForgotFlow(options = {}) {
            const { preserveUsername = true } = options;
            stopForgotCountdown(true);
            clearForgotFeedback();
            if (forgotCodeInput) {
                forgotCodeInput.value = "";
            }
            if (forgotNewPasswordInput) {
                forgotNewPasswordInput.value = "";
                forgotNewPasswordInput.type = "password";
            }
            if (forgotConfirmPasswordInput) {
                forgotConfirmPasswordInput.value = "";
                forgotConfirmPasswordInput.type = "password";
            }
            if (!preserveUsername && forgotUsernameInput) {
                forgotUsernameInput.value = "";
            }
            document.querySelectorAll(".auth-portal-eye").forEach((btn) => {
                btn.classList.remove("is-visible");
                btn.setAttribute("aria-pressed", "false");
            });
            updateForgotUsernamePreview();
        }

        function setForgotPanelOpen(isOpen, options = {}) {
            if (!forgotSection) {
                return;
            }

            const { focus = true, step = currentForgotStep, returnFocus = true, resetFlow = false } = options;
            isForgotPanelOpen = Boolean(isOpen);

            if (isForgotPanelOpen) {
                activeTab = "login";
                currentForgotStep = normalizeForgotStep(step);
                syncForgotInputsFromLogin();
                renderSections();
                renderForgotStep({ focus });
                return;
            }

            if (resetFlow) {
                resetForgotFlow({ preserveUsername: true });
            }
            currentForgotStep = "verify";
            renderSections();
            if (returnFocus && lastPanelTrigger && typeof lastPanelTrigger.focus === "function") {
                lastPanelTrigger.focus();
            }
        }

        function openForgotPanel(trigger) {
            lastPanelTrigger = trigger || document.activeElement;
            clearRuntimeMessage();
            setForgotPanelOpen(true, { step: "verify" });
        }

        function closeForgotPanel() {
            setForgotPanelOpen(false, { focus: false, returnFocus: true, resetFlow: true });
        }

        function clearLoginUsernameIfNeeded() {
            if (preserveLoginUsername || !loginUsernameInput) {
                return;
            }
            loginUsernameInput.value = "";
        }

        function clearCredentialInputs() {
            clearLoginUsernameIfNeeded();
            sensitiveInputs.forEach((input) => {
                input.value = "";
                input.type = "password";
            });
            document.querySelectorAll(".auth-portal-eye").forEach((btn) => {
                btn.classList.remove("is-visible");
                btn.setAttribute("aria-pressed", "false");
            });
            if (forgotCodeInput) {
                forgotCodeInput.value = "";
            }
        }

        function clearPendingTimers() {
            clearTimerIds.forEach((id) => window.clearTimeout(id));
            clearTimerIds.length = 0;
        }

        function clearCredentialsWithRetry() {
            clearPendingTimers();
            clearCredentialInputs();
            [120, 320, 620].forEach((delay) => {
                const timerId = window.setTimeout(clearCredentialInputs, delay);
                clearTimerIds.push(timerId);
            });
        }

        function clearRuntimeMessage() {
            runtimeMessageTimers.forEach((id) => window.clearTimeout(id));
            runtimeMessageTimers.length = 0;
            document.querySelectorAll('.messages-wrap[data-runtime-flash="true"]').forEach((container) => {
                container.remove();
            });
        }

        function normalizeRuntimeMessage(message) {
            return (message || "").trim().replace(/。+$/u, "");
        }

        function showRuntimeMessage(message, type = "error") {
            clearRuntimeMessage();
            const resolvedMessage = normalizeRuntimeMessage(message);

            const container = document.createElement("div");
            container.className = "messages-wrap";
            container.dataset.runtimeFlash = "true";
            container.setAttribute("aria-live", "polite");
            container.setAttribute("aria-atomic", "true");

            const alert = document.createElement("div");
            alert.className = `msg ${type}`;
            alert.setAttribute("role", type === "error" ? "alert" : "status");
            alert.textContent = resolvedMessage;
            container.appendChild(alert);
            document.body.appendChild(container);

            const prefersReducedMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
            const dismissDuration = prefersReducedMotion ? 0 : 220;
            alert.style.setProperty("--msg-dismiss-duration", `${dismissDuration}ms`);
            window.setTimeout(() => {
                alert.classList.add("is-visible");
            }, 10);

            runtimeMessageTimers.push(window.setTimeout(() => {
                alert.classList.remove("is-visible");
                alert.classList.add("is-closing");
                runtimeMessageTimers.push(window.setTimeout(() => {
                    container.remove();
                }, dismissDuration));
            }, 3000));
        }

        function validateLoginForm() {
            const username = loginUsernameInput ? loginUsernameInput.value.trim() : "";
            const password = loginPasswordInput ? loginPasswordInput.value : "";

            if (!username && !password.trim()) {
                showRuntimeMessage("请输入用户名和密码");
                if (loginUsernameInput) {
                    loginUsernameInput.focus();
                }
                return false;
            }
            if (!username) {
                showRuntimeMessage("请输入用户名");
                if (loginUsernameInput) {
                    loginUsernameInput.focus();
                }
                return false;
            }
            if (!password.trim()) {
                showRuntimeMessage("请输入密码");
                if (loginPasswordInput) {
                    loginPasswordInput.focus();
                }
                return false;
            }
            return true;
        }

        function validateForgotVerifyStep() {
            const username = forgotUsernameInput ? forgotUsernameInput.value.trim() : "";
            const code = forgotCodeInput ? forgotCodeInput.value.trim() : "";

            if (!username) {
                setForgotFeedback("verify", "请输入用户名");
                if (forgotUsernameInput) {
                    forgotUsernameInput.focus();
                }
                return false;
            }
            if (username.length < 3) {
                setForgotFeedback("verify", "用户名长度至少为 3 位");
                if (forgotUsernameInput) {
                    forgotUsernameInput.focus();
                }
                return false;
            }
            if (!/^\d{6}$/.test(code)) {
                setForgotFeedback("verify", "请输入 6 位验证码");
                if (forgotCodeInput) {
                    forgotCodeInput.focus();
                }
                return false;
            }

            setForgotFeedback("verify", "");
            updateForgotUsernamePreview();
            return true;
        }

        function validateForgotResetStep() {
            const username = forgotUsernameInput ? forgotUsernameInput.value.trim() : "";
            const newPassword = forgotNewPasswordInput ? forgotNewPasswordInput.value : "";
            const confirmPassword = forgotConfirmPasswordInput ? forgotConfirmPasswordInput.value : "";

            if (!username) {
                setForgotFeedback("reset", "请先完成账号验证");
                currentForgotStep = "verify";
                renderForgotStep();
                return false;
            }
            if (!newPassword.trim()) {
                setForgotFeedback("reset", "请输入新密码");
                if (forgotNewPasswordInput) {
                    forgotNewPasswordInput.focus();
                }
                return false;
            }
            if (newPassword.length < 8) {
                setForgotFeedback("reset", "新密码至少需要 8 位");
                if (forgotNewPasswordInput) {
                    forgotNewPasswordInput.focus();
                }
                return false;
            }
            if (newPassword !== confirmPassword) {
                setForgotFeedback("reset", "两次输入的密码不一致");
                if (forgotConfirmPasswordInput) {
                    forgotConfirmPasswordInput.focus();
                }
                return false;
            }

            setForgotFeedback("reset", "");
            return true;
        }

        tabButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                switchTab(btn.getAttribute("data-tab-target"));
            });
        });

        quickSwitch.forEach((btn) => {
            btn.addEventListener("click", () => {
                switchTab(btn.getAttribute("data-switch-tab"));
            });
        });

        forgotOpenButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                openForgotPanel(btn);
            });
        });

        forgotCloseButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                closeForgotPanel();
            });
        });

        forgotNextButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                if (!validateForgotVerifyStep()) {
                    return;
                }
                currentForgotStep = "reset";
                renderForgotStep();
            });
        });

        forgotPrevButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                setForgotFeedback("reset", "");
                currentForgotStep = "verify";
                renderForgotStep();
            });
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && isForgotPanelOpen) {
                closeForgotPanel();
            }
        });

        document.querySelectorAll(".auth-portal-eye").forEach((btn) => {
            btn.addEventListener("click", () => {
                const targetId = btn.getAttribute("data-target");
                const input = targetId ? document.getElementById(targetId) : null;
                if (!input) return;
                const isVisible = input.type === "password";
                input.type = isVisible ? "text" : "password";
                btn.classList.toggle("is-visible", isVisible);
                btn.setAttribute("aria-pressed", isVisible ? "true" : "false");
            });
        });

        formInputs.forEach((input) => {
            ["input", "keydown", "paste", "change"].forEach((eventName) => {
                input.addEventListener(eventName, clearPendingTimers, { once: true });
            });
        });

        if (forgotUsernameInput) {
            ["input", "change"].forEach((eventName) => {
                forgotUsernameInput.addEventListener(eventName, () => {
                    updateForgotUsernamePreview();
                    setForgotFeedback("verify", "");
                });
            });
        }

        if (forgotCodeInput) {
            ["input", "change"].forEach((eventName) => {
                forgotCodeInput.addEventListener(eventName, () => {
                    forgotCodeInput.value = forgotCodeInput.value.replace(/\D+/g, "").slice(0, 6);
                    setForgotFeedback("verify", "");
                });
            });
        }

        if (forgotNewPasswordInput) {
            ["input", "change"].forEach((eventName) => {
                forgotNewPasswordInput.addEventListener(eventName, () => {
                    setForgotFeedback("reset", "");
                });
            });
        }

        if (forgotConfirmPasswordInput) {
            ["input", "change"].forEach((eventName) => {
                forgotConfirmPasswordInput.addEventListener(eventName, () => {
                    setForgotFeedback("reset", "");
                });
            });
        }

        if (forgotSendCodeButton) {
            forgotSendCodeButton.addEventListener("click", () => {
                const username = forgotUsernameInput ? forgotUsernameInput.value.trim() : "";
                if (!username) {
                    setForgotFeedback("verify", "请先输入用户名，再发送验证码");
                    if (forgotUsernameInput) {
                        forgotUsernameInput.focus();
                    }
                    return;
                }
                if (username.length < 3) {
                    setForgotFeedback("verify", "用户名长度至少为 3 位");
                    if (forgotUsernameInput) {
                        forgotUsernameInput.focus();
                    }
                    return;
                }
                setForgotFeedback("verify", "");
                startForgotCountdown();
                if (forgotCodeInput) {
                    forgotCodeInput.focus();
                }
            });
        }

        if (loginUsernameInput) {
            ["input", "change"].forEach((eventName) => {
                loginUsernameInput.addEventListener(eventName, clearRuntimeMessage);
            });
        }

        if (loginPasswordInput) {
            ["input", "change"].forEach((eventName) => {
                loginPasswordInput.addEventListener(eventName, clearRuntimeMessage);
            });
        }

        clearCredentialsWithRetry();
        renderSections();
        renderForgotStep({ focus: false });
        if (isForgotPanelOpen) {
            renderForgotStep();
        } else {
            setUnderline(getActiveTabButton());
        }
        if (loginForm) {
            loginForm.addEventListener("submit", (event) => {
                if (!validateLoginForm()) {
                    event.preventDefault();
                    return;
                }
                try {
                    sessionStorage.setItem(AUTH_TAB_KEY, String(Date.now()));
                } catch (error) {
                    // Ignore sessionStorage access issues and allow server-side auth flow.
                }
            });
        }
        window.addEventListener("pageshow", (event) => {
            if (event.persisted) {
                clearRuntimeMessage();
                clearCredentialsWithRetry();
                return;
            }
            const navEntry = window.performance && typeof window.performance.getEntriesByType === "function"
                ? window.performance.getEntriesByType("navigation")[0]
                : null;
            if (navEntry && navEntry.type === "reload") {
                clearRuntimeMessage();
                clearCredentialsWithRetry();
            }
        });
        if (forgotForm) {
            forgotForm.addEventListener("submit", (event) => {
                if (currentForgotStep !== "reset") {
                    event.preventDefault();
                    if (validateForgotVerifyStep()) {
                        currentForgotStep = "reset";
                        renderForgotStep();
                    }
                    return;
                }
                if (!validateForgotResetStep()) {
                    event.preventDefault();
                    return;
                }
                clearPendingTimers();
                stopForgotCountdown(false);
            });
        }
        window.addEventListener("resize", () => {
            if (!isForgotPanelOpen) {
                setUnderline(getActiveTabButton());
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", setupAuthPage, { once: true });
    } else {
        setupAuthPage();
    }
})();
