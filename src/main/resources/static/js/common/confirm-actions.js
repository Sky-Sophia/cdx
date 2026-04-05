(() => {
    "use strict";

    let modal;
    let titleNode;
    let messageNode;
    let cancelBtn;
    let confirmBtn;
    let activePayload = null;

    function ensureModal() {
        if (modal) {
            return true;
        }
        modal = document.getElementById("globalConfirmModal");
        if (!modal) {
            return false;
        }
        titleNode = document.getElementById("globalConfirmTitle");
        messageNode = document.getElementById("globalConfirmMessage");
        cancelBtn = document.getElementById("globalConfirmCancel");
        confirmBtn = document.getElementById("globalConfirmOk");

        cancelBtn?.addEventListener("click", closeModal);
        modal.addEventListener("click", (event) => {
            if (event.target === modal) {
                closeModal();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("is-open")) {
                closeModal();
            }
        });
        confirmBtn?.addEventListener("click", handleConfirm);
        return true;
    }

    function openModal(payload) {
        if (!ensureModal()) {
            return false;
        }
        activePayload = payload;
        if (titleNode) {
            titleNode.textContent = payload.title || "二次确认";
        }
        if (messageNode) {
            messageNode.textContent = payload.message || "确定要执行此操作吗？";
        }
        if (confirmBtn) {
            confirmBtn.textContent = payload.confirmText || "确定";
            confirmBtn.classList.toggle("is-danger", payload.tone === "danger");
        }
        if (cancelBtn) {
            cancelBtn.textContent = payload.cancelText || "取消";
        }
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.classList.add("confirm-modal-open");
        window.setTimeout(() => confirmBtn?.focus(), 0);
        return true;
    }

    function closeModal() {
        if (!modal) {
            return;
        }
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("confirm-modal-open");
        activePayload = null;
    }

    function handleConfirm() {
        if (!activePayload) {
            closeModal();
            return;
        }
        const payload = activePayload;
        closeModal();

        if (payload.type === "form" && payload.form) {
            HTMLFormElement.prototype.submit.call(payload.form);
            return;
        }
        if (payload.type === "link" && payload.href) {
            window.location.href = payload.href;
            return;
        }
        if (payload.type === "button" && payload.button) {
            payload.button.setAttribute("data-confirm-approved", "true");
            payload.button.click();
            payload.button.removeAttribute("data-confirm-approved");
        }
    }

    function buildPayloadFromElement(element, fallbackType) {
        return {
            type: fallbackType,
            form: fallbackType === "form" ? element : null,
            button: fallbackType === "button" ? element : null,
            href: fallbackType === "link" ? element.getAttribute("href") : null,
            title: element.getAttribute("data-confirm-title") || "操作确认",
            message: element.getAttribute("data-confirm-message") || "确定要执行此操作吗？",
            confirmText: element.getAttribute("data-confirm-confirm-text") || "确定",
            cancelText: element.getAttribute("data-confirm-cancel-text") || "取消",
            tone: element.getAttribute("data-confirm-tone") || "default"
        };
    }

    document.addEventListener("submit", (event) => {
        const form = event.target.closest("form[data-confirm-message]");
        if (!form) {
            return;
        }
        event.preventDefault();
        openModal(buildPayloadFromElement(form, "form"));
    });

    document.addEventListener("click", (event) => {
        const actionLink = event.target.closest("a[data-confirm-message]");
        if (actionLink) {
            event.preventDefault();
            openModal(buildPayloadFromElement(actionLink, "link"));
            return;
        }

        const actionButton = event.target.closest("button[data-confirm-message]");
        if (actionButton && actionButton.getAttribute("data-confirm-approved") !== "true") {
            event.preventDefault();
            openModal(buildPayloadFromElement(actionButton, "button"));
        }
    });
})();

