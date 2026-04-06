(() => {
    "use strict";

    const AUTH_TAB_KEY = "pms_auth_tab";

    function inProtectedArea() {
        const path = window.location.pathname || "";
        return path.startsWith("/admin") || path.startsWith("/profile");
    }

    /**
     * If the page rendered in a protected area, the server-side interceptor already
     * verified the session. Ensure the sessionStorage marker exists so that future
     * client-side checks (e.g. after bfcache restore) can rely on it.
     *
     * Previous implementation redirected to /logout when the marker was absent,
     * which incorrectly destroyed valid server sessions when users opened new tabs,
     * bookmarks, or navigated to form pages.
     */
    function ensureTabMarker() {
        if (!inProtectedArea()) {
            return;
        }
        try {
            if (!sessionStorage.getItem(AUTH_TAB_KEY)) {
                sessionStorage.setItem(AUTH_TAB_KEY, String(Date.now()));
            }
        } catch (error) {
            // sessionStorage unavailable — ignore silently
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", ensureTabMarker, { once: true });
    } else {
        ensureTabMarker();
    }
})();

