(() => {
    "use strict";

    function toPositiveNumber(value, fallback) {
        const number = Number(value);
        return Number.isFinite(number) && number >= 0 ? number : fallback;
    }

    function normalizeMessageText(text) {
        return (text || '').trim().replace(/。+$/u, '');
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
            const duration = prefersReducedMotion ? 0 : toPositiveNumber(container.dataset.dismissDuration, 220);

            messages.forEach((message) => {
                message.textContent = normalizeMessageText(message.textContent);
                message.style.setProperty('--msg-dismiss-duration', `${duration}ms`);
                window.setTimeout(() => {
                    message.classList.add('is-visible');
                }, 10);
            });

            window.setTimeout(() => {
                messages.forEach((message) => {
                    message.classList.remove('is-visible');
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

