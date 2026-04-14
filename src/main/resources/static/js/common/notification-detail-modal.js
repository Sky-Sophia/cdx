(() => {
    "use strict";

    function setupNotificationDetailModal() {
        const notificationCommon = window.NotificationCommon;
        const modal = document.querySelector("[data-notification-detail-modal]");
        if (!modal || !notificationCommon) {
            return;
        }

        const titleElement = modal.querySelector("[data-notification-detail-title]");
        const typeElement = modal.querySelector("[data-notification-detail-type]");
        const senderElement = modal.querySelector("[data-notification-detail-sender]");
        const timeElement = modal.querySelector("[data-notification-detail-time]");
        const contentElement = modal.querySelector("[data-notification-detail-content]");
        const closeButtons = modal.querySelectorAll("[data-notification-detail-close]");

        const state = {
            lastActiveElement: null
        };

        function populate(item, options) {
            const resolvedItem = item || {};
            const resolvedOptions = options || {};
            const typeMeta = notificationCommon.resolveTypeMeta(resolvedItem.msgType);

            if (titleElement) {
                titleElement.textContent = resolvedOptions.title || "通知详情";
            }
            if (typeElement) {
                typeElement.textContent = typeMeta.label;
                typeElement.dataset.tone = typeMeta.tone;
            }
            if (senderElement) {
                senderElement.textContent = String(resolvedItem.sender || "").trim() || "系统消息";
            }
            if (timeElement) {
                timeElement.textContent = notificationCommon.formatRelativeTime(resolvedItem.sendTime, {
                    includeTimeInAbsolute: true
                });
            }
            if (contentElement) {
                contentElement.textContent = String(resolvedItem.content || "").trim() || "暂无消息内容";
                contentElement.scrollTop = 0;
            }
        }

        function open(item, options) {
            if (!item) {
                return;
            }

            state.lastActiveElement = document.activeElement instanceof HTMLElement
                ? document.activeElement
                : null;

            populate(item, options);
            modal.hidden = false;
            modal.setAttribute("aria-hidden", "false");
            document.body.classList.add("notification-detail-modal-open");

            const primaryCloseButton = modal.querySelector("[data-notification-detail-close]");
            if (primaryCloseButton && typeof primaryCloseButton.focus === "function") {
                primaryCloseButton.focus({ preventScroll: true });
            }
        }

        function close() {
            if (modal.hidden) {
                return;
            }

            modal.hidden = true;
            modal.setAttribute("aria-hidden", "true");
            document.body.classList.remove("notification-detail-modal-open");

            if (state.lastActiveElement && typeof state.lastActiveElement.focus === "function") {
                state.lastActiveElement.focus({ preventScroll: true });
            }
            state.lastActiveElement = null;
        }

        closeButtons.forEach((button) => {
            button.addEventListener("click", close);
        });

        modal.addEventListener("click", (event) => {
            if (event.target === modal) {
                close();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !modal.hidden) {
                close();
            }
        });

        window.notificationDetailModal = {
            open,
            close,
            isOpen() {
                return !modal.hidden;
            }
        };

        window.addEventListener("notification:detail-open", (event) => {
            const detail = event.detail || {};
            if (detail.item) {
                open(detail.item, detail.options || {});
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", setupNotificationDetailModal, { once: true });
    } else {
        setupNotificationDetailModal();
    }
})();
