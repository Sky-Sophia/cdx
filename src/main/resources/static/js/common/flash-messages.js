(() => {
    "use strict";

    function toPositiveNumber(value, fallback) {
        const number = Number(value);
        return Number.isFinite(number) && number >= 0 ? number : fallback;
    }

    function initFlashMessages() {
        document.querySelectorAll('.messages-wrap[data-auto-dismiss="true"]').forEach((container) => {
            const messages = Array.from(container.querySelectorAll('.msg'));
            if (messages.length === 0) {
                container.remove();
                return;
            }

            const delay = toPositiveNumber(container.dataset.dismissDelay, 3000);
            const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
            const duration = prefersReducedMotion ? 0 : toPositiveNumber(container.dataset.dismissDuration, 320);

            messages.forEach((message) => {
                message.style.setProperty('--msg-dismiss-duration', `${duration}ms`);
            });

            window.setTimeout(() => {
                messages.forEach((message) => {
                    message.classList.add('is-closing');
                });

                window.setTimeout(() => {
                    container.remove();
                }, duration);
            }, delay);
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initFlashMessages, { once: true });
    } else {
        initFlashMessages();
    }
})();

