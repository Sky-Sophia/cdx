(() => {
    "use strict";


    const VALID_TABS = new Set(["dashboard", "units", "residents", "work-orders", "bills", "users"]);

    function isManagementPath() {
        const pathname = (window.location.pathname || "").replace(/\/+$/, "");
        return pathname === "/admin/management" || pathname.startsWith("/admin/management/");
    }

    function shouldHandleTabClick(event) {
        return event.button === 0
            && !event.metaKey
            && !event.ctrlKey
            && !event.shiftKey
            && !event.altKey;
    }

    function initManagementTabs() {
        const main = document.querySelector(".app-main");
        if (!main || !main.dataset.currentTab || !isManagementPath()) {
            return;
        }

        const paneByTab = new Map();
        document.querySelectorAll(".mgmt-pane[data-pane-tab]").forEach((pane) => {
            paneByTab.set(pane.dataset.paneTab, pane);
        });
        if (paneByTab.size === 0) {
            return;
        }

        const navTabLinks = Array.from(document.querySelectorAll("[data-management-tab]"));
        let activeTab = "";

        function updateNavHighlight(tab) {
            navTabLinks.forEach((link) => {
                const isActive = link.dataset.managementTab === tab;
                if (isActive !== link.classList.contains("is-active")) {
                    link.classList.toggle("is-active", isActive);
                }
            });
        }

        function updateHistory(tab) {
            const nextUrl = new URL(window.location.href);
            if (nextUrl.searchParams.get("tab") === tab) {
                return;
            }
            nextUrl.searchParams.set("tab", tab);
            window.history.replaceState(null, "", nextUrl.toString());
        }

        function switchTab(tab) {
            const target = VALID_TABS.has(tab) ? tab : "dashboard";
            if (target === activeTab) {
                return;
            }

            const prevPane = paneByTab.get(activeTab);
            const nextPane = paneByTab.get(target);
            if (prevPane && prevPane !== nextPane) {
                prevPane.classList.add("is-hidden");
            }
            if (nextPane) {
                nextPane.classList.remove("is-hidden");
            }

            activeTab = target;
            main.dataset.currentTab = target;
            updateNavHighlight(target);
            updateHistory(target);

            // 切换到 dashboard 时，通知 ECharts 图表重新计算尺寸
            // （从 display:none 恢复后，容器尺寸才可被正确读取）
            if (target === "dashboard" && typeof window.__dashboardChartsResize === "function") {
                requestAnimationFrame(() => window.__dashboardChartsResize());
            }

            if (typeof window.refreshTopNavUnderline === "function") {
                window.refreshTopNavUnderline(true);
            }
        }

        navTabLinks.forEach((link) => {
            link.addEventListener("click", (event) => {
                if (!shouldHandleTabClick(event)) {
                    return;
                }
                event.preventDefault();
                switchTab(link.dataset.managementTab);
            });
        });

        switchTab(main.dataset.currentTab || "dashboard");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initManagementTabs, { once: true });
    } else {
        initManagementTabs();
    }
})();
