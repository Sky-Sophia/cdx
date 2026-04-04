(() => {
    "use strict";

    function initSlideTabs() {
        const containers = document.querySelectorAll(".top-nav.slide-tabs");
        if (!containers.length) {
            return;
        }

        const refreshers = [];
        containers.forEach((container) => {
            if (!container || container.dataset.slideInited === "1") {
                return;
            }

            const underline = container.querySelector(".slide-underline");
            const tabs = Array.from(container.querySelectorAll("a, button"))
                .filter((item) => !item.classList.contains("slide-underline"));
            if (!underline || tabs.length === 0) {
                return;
            }

            const setUnderline = (tab, instant = false) => {
                if (!tab) {
                    return;
                }
                underline.classList.toggle("is-instant", instant);
                underline.style.width = `${tab.offsetWidth}px`;
                underline.style.left = `${tab.offsetLeft}px`;
                if (instant) {
                    requestAnimationFrame(() => underline.classList.remove("is-instant"));
                }
            };

            const getActiveTab = () => tabs.find((item) => item.classList.contains("is-active")) || tabs[0];
            let rafId = 0;
            const refresh = (instant = false) => {
                if (rafId) {
                    cancelAnimationFrame(rafId);
                }
                rafId = requestAnimationFrame(() => setUnderline(getActiveTab(), instant));
            };

            container.dataset.slideInited = "1";
            refreshers.push(refresh);
            refresh(true);

            if (document.fonts && document.fonts.ready) {
                document.fonts.ready.then(() => refresh(true)).catch(() => {
                });
            }
        });

        const refreshAll = (instant = false) => {
            refreshers.forEach((fn) => fn(instant));
        };
        window.refreshTopNavUnderline = refreshAll;
        window.addEventListener("resize", () => refreshAll(true), { passive: true });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initSlideTabs, { once: true });
    } else {
        initSlideTabs();
    }
})();

