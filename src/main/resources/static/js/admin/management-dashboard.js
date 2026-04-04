(() => {
    "use strict";

    function clampPercent(value) {
        const number = Number(value);
        if (Number.isNaN(number)) {
            return 0;
        }
        return Math.max(0, Math.min(number, 100));
    }

    function initDashboardVisuals() {
        const donut = document.querySelector(".dashboard-donut[data-progress]");
        if (donut) {
            donut.style.setProperty("--progress", String(clampPercent(donut.dataset.progress)));
        }

        document.querySelectorAll(".dashboard-bar-fill[data-width]").forEach((bar) => {
            bar.style.width = `${clampPercent(bar.dataset.width)}%`;
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initDashboardVisuals, { once: true });
    } else {
        initDashboardVisuals();
    }
})();

