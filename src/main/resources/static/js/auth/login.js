(() => {
    "use strict";

    const AUTH_TAB_KEY = "pms_auth_tab";

    function setupAuthPage() {
        const card = document.querySelector(".auth-portal-card");
        if (!card) {
            return;
        }

        const initialTab = card.getAttribute("data-initial-tab") || "login";
        const initialForgotOpen = card.getAttribute("data-forgot-open") === "true";
        const tabButtons = document.querySelectorAll(".auth-tab-btn");
        const tabNav = document.querySelector(".auth-portal-tabs");
        const tabUnderline = tabNav ? tabNav.querySelector(".slide-underline") : null;
        const loginSection = document.querySelector(".auth-portal-section-login");
        const registerSection = document.querySelector(".auth-portal-section-register");
        const quickSwitch = document.querySelectorAll("[data-switch-tab]");
        const forgotModal = document.getElementById("forgotPasswordModal");
        const forgotForm = document.getElementById("forgotForm");
        const forgotUsernameInput = document.getElementById("forgot-username");
        const forgotOpenButtons = document.querySelectorAll("[data-open-forgot-modal]");
        const forgotCloseButtons = document.querySelectorAll("[data-close-forgot-modal]");
        const loginForm = document.getElementById("loginForm");
        const loginUsernameInput = document.getElementById("login-username");
        const loginPasswordInput = document.getElementById("login-password");
        const preserveLoginUsername = loginForm && loginForm.getAttribute("data-preserve-username") === "true";
        const formInputs = document.querySelectorAll(
            "#login-username, #login-password, #register-username, #register-password, #register-confirm-password, #forgot-username, #forgot-new-password, #forgot-confirm-password"
        );
        const sensitiveInputs = document.querySelectorAll(
            "#login-password, #register-password, #register-confirm-password, #forgot-new-password, #forgot-confirm-password"
        );
        const clearTimerIds = [];
        const runtimeMessageTimers = [];
        let lastModalTrigger = null;

        function setUnderline(targetBtn) {
            if (!tabUnderline || !targetBtn) return;
            tabUnderline.style.width = `${targetBtn.offsetWidth}px`;
            tabUnderline.style.left = `${targetBtn.offsetLeft}px`;
        }

        function switchTab(tab) {
            const target = tab === "register" ? "register" : "login";
            if (target !== "login") {
                clearRuntimeMessage();
            }
            if (loginSection) {
                loginSection.classList.toggle("is-hidden", target !== "login");
            }
            if (registerSection) {
                registerSection.classList.toggle("is-hidden", target !== "register");
            }
            tabButtons.forEach((btn) => {
                const isActive = btn.getAttribute("data-tab-target") === target;
                btn.classList.toggle("is-active", isActive);
            });
            setUnderline(Array.from(tabButtons).find((btn) => btn.classList.contains("is-active")));
        }

        function setForgotModalOpen(isOpen) {
            if (!forgotModal) {
                return;
            }
            forgotModal.classList.toggle("is-open", isOpen);
            forgotModal.setAttribute("aria-hidden", isOpen ? "false" : "true");
            document.body.classList.toggle("has-modal-open", isOpen);
            if (isOpen && forgotUsernameInput) {
                window.setTimeout(() => {
                    forgotUsernameInput.focus();
                    forgotUsernameInput.select();
                }, 40);
            }
        }

        function openForgotModal(trigger) {
            lastModalTrigger = trigger || document.activeElement;
            switchTab("login");
            setForgotModalOpen(true);
        }

        function closeForgotModal() {
            setForgotModalOpen(false);
            if (lastModalTrigger && typeof lastModalTrigger.focus === "function") {
                lastModalTrigger.focus();
            }
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

        function showRuntimeMessage(message, type = "error") {
            clearRuntimeMessage();

            const container = document.createElement("div");
            container.className = "messages-wrap";
            container.dataset.runtimeFlash = "true";
            container.setAttribute("aria-live", "polite");
            container.setAttribute("aria-atomic", "true");

            const alert = document.createElement("div");
            alert.className = `msg ${type}`;
            alert.setAttribute("role", type === "error" ? "alert" : "status");
            alert.textContent = message;
            container.appendChild(alert);
            document.body.appendChild(container);

            const prefersReducedMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
            const dismissDuration = prefersReducedMotion ? 0 : 320;
            alert.style.setProperty("--msg-dismiss-duration", `${dismissDuration}ms`);

            runtimeMessageTimers.push(window.setTimeout(() => {
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
                openForgotModal(btn);
            });
        });

        forgotCloseButtons.forEach((btn) => {
            btn.addEventListener("click", () => {
                closeForgotModal();
            });
        });

        if (forgotModal) {
            forgotModal.addEventListener("click", (event) => {
                if (event.target === forgotModal) {
                    closeForgotModal();
                }
            });
        }

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && forgotModal && forgotModal.classList.contains("is-open")) {
                closeForgotModal();
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
        switchTab(initialTab);
        setForgotModalOpen(initialForgotOpen);
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
            forgotForm.addEventListener("submit", () => {
                clearPendingTimers();
            });
        }
        window.addEventListener("resize", () => {
            setUnderline(Array.from(tabButtons).find((btn) => btn.classList.contains("is-active")));
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", setupAuthPage, { once: true });
    } else {
        setupAuthPage();
    }
})();
