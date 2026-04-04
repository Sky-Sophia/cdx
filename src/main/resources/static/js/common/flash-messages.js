(() => {
    "use strict";

    function initFlashMessages() {
        document.querySelectorAll('.messages-wrap[data-auto-dismiss="true"]').forEach((container) => {
            const messages = Array.from(container.querySelectorAll('.msg'));
            if (messages.length === 0) {
                return;
            }

            const delay = Number(container.dataset.dismissDelay || 2200);
            const duration = Number(container.dataset.dismissDuration || 280);

            messages.forEach((message, index) => {
                const hideAfter = delay + index * 120;
                window.setTimeout(() => {
                    message.classList.add("is-hiding");
                    window.setTimeout(() => {
                        message.remove();
                        if (!container.querySelector('.msg')) {
                            container.remove();
                        }
                    }, duration);
                }, hideAfter);
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initFlashMessages, { once: true });
    } else {
        initFlashMessages();
    }
})();

