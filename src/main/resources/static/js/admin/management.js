(() => {
    "use strict";


    const VALID_TABS = new Set(["dashboard", "units", "residents", "work-orders", "bills", "users"]);
    const SCROLL_STATE_KEY = "pms_management_scroll_state";
    const SCROLL_STATE_TTL = 15000;
    const TAB_TO_SECTION = {
        dashboard: "dashboard",
        units: "assets",
        residents: "assets",
        "work-orders": "services",
        bills: "services",
        users: "system"
    };
    const DEFAULT_TAB_BY_SECTION = {
        dashboard: "dashboard",
        assets: "units",
        services: "work-orders",
        system: "users"
    };

    let pendingScrollState = null;
    let hasRestoredPendingScroll = false;

    function getManagementPath() {
        return (window.location.pathname || "").replace(/\/+$/, "");
    }

    function isManagementPath() {
        const pathname = getManagementPath();
        return pathname === "/admin/management" || pathname.startsWith("/admin/management/");
    }

    function shouldHandleTabClick(event) {
        return event.button === 0
            && !event.metaKey
            && !event.ctrlKey
            && !event.shiftKey
            && !event.altKey;
    }

    function getRequestedManagementTab() {
        const url = new URL(window.location.href);
        return url.searchParams.get("tab") || "dashboard";
    }

    function submitForm(form) {
        if (!form) {
            return;
        }
        if (typeof form.requestSubmit === "function") {
            form.requestSubmit();
            return;
        }
        form.submit();
    }

    function getCurrentManagementTab() {
        const main = document.querySelector(".app-main");
        if (main && main.dataset.currentTab) {
            return main.dataset.currentTab;
        }
        return getRequestedManagementTab();
    }

    function saveManagementScrollPosition() {
        if (!isManagementPath()) {
            return;
        }
        try {
            sessionStorage.setItem(SCROLL_STATE_KEY, JSON.stringify({
                path: getManagementPath(),
                tab: getCurrentManagementTab(),
                x: window.scrollX || window.pageXOffset || 0,
                y: window.scrollY || window.pageYOffset || 0,
                ts: Date.now()
            }));
        } catch (error) {
            // Ignore sessionStorage failures and fall back to default browser behavior.
        }
    }

    function clearPendingManagementScrollState() {
        pendingScrollState = null;
        try {
            sessionStorage.removeItem(SCROLL_STATE_KEY);
        } catch (error) {
            // Ignore sessionStorage cleanup failures.
        }
    }

    function loadPendingManagementScrollState() {
        if (!isManagementPath()) {
            return null;
        }
        try {
            const raw = sessionStorage.getItem(SCROLL_STATE_KEY);
            if (!raw) {
                return null;
            }
            const state = JSON.parse(raw);
            if (!state || state.path !== getManagementPath()) {
                clearPendingManagementScrollState();
                return null;
            }
            if (Date.now() - Number(state.ts || 0) > SCROLL_STATE_TTL) {
                clearPendingManagementScrollState();
                return null;
            }
            const requestedTab = getRequestedManagementTab();
            if (state.tab && requestedTab && state.tab !== requestedTab) {
                clearPendingManagementScrollState();
                return null;
            }
            return state;
        } catch (error) {
            clearPendingManagementScrollState();
            return null;
        }
    }

    function consumeManagementScrollPosition() {
        if (hasRestoredPendingScroll || !pendingScrollState) {
            return null;
        }
        hasRestoredPendingScroll = true;
        const state = pendingScrollState;
        clearPendingManagementScrollState();
        return state;
    }

    function restoreManagementScrollPosition() {
        const state = consumeManagementScrollPosition();
        if (!state) {
            return;
        }

        const x = Number.isFinite(Number(state.x)) ? Number(state.x) : 0;
        const y = Number.isFinite(Number(state.y)) ? Number(state.y) : 0;
        window.scrollTo(x, y);
    }

    function initManagementScrollPersistence() {
        if (!isManagementPath()) {
            return;
        }

        restoreManagementScrollPosition();

        document.addEventListener("submit", (event) => {
            const target = event.target;
            const form = target instanceof HTMLFormElement
                ? target
                : (target instanceof Element ? target.closest("form") : null);
            if (!form || !form.closest(".app-main")) {
                return;
            }
            saveManagementScrollPosition();
        }, true);

        document.addEventListener("click", (event) => {
            const target = event.target instanceof Element ? event.target : null;
            const link = target
                ? target.closest(".pagination-bar .page-link[href], .filter-action-reset[href]")
                : null;
            if (!link || !link.closest(".app-main") || !shouldHandleTabClick(event)) {
                return;
            }
            saveManagementScrollPosition();
        }, true);
    }

    function initManagementAutoFilters() {
        if (!isManagementPath()) {
            return;
        }

        document.querySelectorAll(".filter-row[data-auto-submit-filters='true']").forEach((form) => {
            form.querySelectorAll("[data-auto-submit-filter='true']").forEach((field) => {
                field.addEventListener("change", () => submitForm(form));
            });
        });
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

        const secondaryNavLinks = Array.from(document.querySelectorAll(".side-nav [data-management-tab]"));
        const primaryNavLinks = Array.from(document.querySelectorAll(".top-nav [data-management-section]"));
        const sectionPanels = Array.from(document.querySelectorAll(".side-nav-group[data-management-section-panel]"));
        const lastTabBySection = new Map();
        let activeTab = "";

        function getSectionByTab(tab) {
            return TAB_TO_SECTION[tab] || "dashboard";
        }

        function updateSecondaryNavHighlight(tab) {
            secondaryNavLinks.forEach((link) => {
                const isActive = link.dataset.managementTab === tab;
                if (isActive !== link.classList.contains("is-active")) {
                    link.classList.toggle("is-active", isActive);
                }
            });
        }

        function updatePrimaryNavHighlight(section) {
            primaryNavLinks.forEach((link) => {
                const isActive = link.dataset.managementSection === section;
                if (isActive !== link.classList.contains("is-active")) {
                    link.classList.toggle("is-active", isActive);
                }
            });
        }

        function updateSectionPanels(section) {
            sectionPanels.forEach((panel) => {
                const isMatch = panel.dataset.managementSectionPanel === section;
                panel.classList.toggle("is-hidden", !isMatch);
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

        function switchTab(tab, instantUnderline = false) {
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
            const section = getSectionByTab(target);
            lastTabBySection.set(section, target);
            main.dataset.currentTab = target;
            updateSecondaryNavHighlight(target);
            updatePrimaryNavHighlight(section);
            updateSectionPanels(section);
            updateHistory(target);

            // 切换到 dashboard 时，通知 ECharts 图表重新计算尺寸
            // （从 display:none 恢复后，容器尺寸才可被正确读取）
            if (target === "dashboard" && typeof window.__dashboardChartsResize === "function") {
                requestAnimationFrame(() => window.__dashboardChartsResize());
            }

            if (typeof window.refreshTopNavUnderline === "function") {
                window.refreshTopNavUnderline(instantUnderline);
            }
        }

        function switchSection(section, instantUnderline = false) {
            const target = lastTabBySection.get(section) || DEFAULT_TAB_BY_SECTION[section] || "dashboard";
            switchTab(target, instantUnderline);
        }

        secondaryNavLinks.forEach((link) => {
            link.addEventListener("click", (event) => {
                if (!shouldHandleTabClick(event)) {
                    return;
                }
                event.preventDefault();
                switchTab(link.dataset.managementTab);
            });
        });

        primaryNavLinks.forEach((link) => {
            link.addEventListener("click", (event) => {
                if (!shouldHandleTabClick(event)) {
                    return;
                }
                event.preventDefault();
                switchSection(link.dataset.managementSection);
            });
        });

        switchTab(main.dataset.currentTab || "dashboard", true);
    }

    function init() {
        initManagementScrollPersistence();
        initManagementTabs();
        initManagementAutoFilters();
    }

    if (isManagementPath()) {
        pendingScrollState = loadPendingManagementScrollState();
        if (window.history && "scrollRestoration" in window.history) {
            window.history.scrollRestoration = "manual";
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
