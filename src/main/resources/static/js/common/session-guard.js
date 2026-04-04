(() => {
    "use strict";

    const AUTH_TAB_KEY = "pms_auth_tab";

    function inProtectedArea() {
        const path = window.location.pathname || "";
        return path.startsWith("/admin") || path.startsWith("/profile");
    }

    function enforceTabSession() {
        if (!inProtectedArea()) {
            return;
        }
        try {
            const marker = sessionStorage.getItem(AUTH_TAB_KEY);
            if (!marker) {
                window.location.replace("/logout");
            }
        } catch (error) {
            window.location.replace("/logout");
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", enforceTabSession, { once: true });
    } else {
        enforceTabSession();
    }
})();

