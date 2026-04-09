(() => {
    "use strict";

    function updateViewportScrollbarWidth() {
        const scrollbarWidth = Math.max(window.innerWidth - document.documentElement.clientWidth, 0);
        document.documentElement.style.setProperty("--viewport-scrollbar-width", `${scrollbarWidth}px`);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", updateViewportScrollbarWidth, { once: true });
    } else {
        updateViewportScrollbarWidth();
    }

    window.addEventListener("load", updateViewportScrollbarWidth);
    window.addEventListener("resize", updateViewportScrollbarWidth, { passive: true });
})();

