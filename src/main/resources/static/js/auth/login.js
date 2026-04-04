(() => {
    "use strict";

    const AUTH_TAB_KEY = "pms_auth_tab";

    function setupAuthPage() {
        const card = document.querySelector(".auth-portal-card");
        if (!card) {
            return;
        }

        const initialTab = card.getAttribute("data-initial-tab") || "login";
        const tabButtons = document.querySelectorAll(".auth-tab-btn");
        const tabNav = document.querySelector(".auth-portal-tabs");
        const tabUnderline = tabNav ? tabNav.querySelector(".slide-underline") : null;
        const loginSection = document.querySelector(".auth-portal-section-login");
        const registerSection = document.querySelector(".auth-portal-section-register");
        const forgotSection = document.querySelector(".auth-portal-section-forgot");
        const quickSwitch = document.querySelectorAll("[data-switch-tab]");
        const loginForm = document.getElementById("loginForm");
        const credentialInputs = document.querySelectorAll(
            "#login-username, #login-password, #register-username, #register-password, #register-confirm-password, #forgot-username, #forgot-new-password, #forgot-confirm-password"
        );
        const clearTimerIds = [];

        function setUnderline(targetBtn) {
            if (!tabUnderline || !targetBtn) return;
            tabUnderline.style.width = `${targetBtn.offsetWidth}px`;
            tabUnderline.style.left = `${targetBtn.offsetLeft}px`;
        }

        function switchTab(tab) {
            const target = (tab === "register" || tab === "forgot") ? tab : "login";
            if (loginSection) {
                loginSection.classList.toggle("is-hidden", target !== "login");
            }
            if (registerSection) {
                registerSection.classList.toggle("is-hidden", target !== "register");
            }
            if (forgotSection) {
                forgotSection.classList.toggle("is-hidden", target !== "forgot");
            }
            // For tab underline, map forgot back to login tab highlight
            const underlineTarget = target === "forgot" ? "login" : target;
            tabButtons.forEach((btn) => {
                const isActive = btn.getAttribute("data-tab-target") === underlineTarget;
                btn.classList.toggle("is-active", isActive);
            });
            setUnderline(Array.from(tabButtons).find((btn) => btn.classList.contains("is-active")));
        }

        function clearCredentials() {
            credentialInputs.forEach((input) => {
                input.value = "";
                if (input.id.includes("password")) {
                    input.type = "password";
                }
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
            clearCredentials();
            [120, 320, 620].forEach((delay) => {
                const timerId = window.setTimeout(clearCredentials, delay);
                clearTimerIds.push(timerId);
            });
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

        credentialInputs.forEach((input) => {
            ["input", "keydown", "paste", "change"].forEach((eventName) => {
                input.addEventListener(eventName, clearPendingTimers, { once: true });
            });
        });

        clearCredentialsWithRetry();
        switchTab(initialTab);
        if (loginForm) {
            loginForm.addEventListener("submit", () => {
                try {
                    sessionStorage.setItem(AUTH_TAB_KEY, String(Date.now()));
                } catch (error) {
                    // Ignore sessionStorage access issues and allow server-side auth flow.
                }
            });
        }
        window.addEventListener("pageshow", (event) => {
            if (event.persisted) {
                clearCredentialsWithRetry();
                return;
            }
            const navEntry = window.performance && typeof window.performance.getEntriesByType === "function"
                ? window.performance.getEntriesByType("navigation")[0]
                : null;
            if (navEntry && navEntry.type === "reload") {
                clearCredentialsWithRetry();
            }
        });
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
