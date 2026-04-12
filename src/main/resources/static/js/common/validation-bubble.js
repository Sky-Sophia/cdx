(() => {
    "use strict";

    const FIELD_INVALID_CLASS = "validation-bubble-field-invalid";
    const CYCLE_WINDOW_MS = 80;
    const MESSAGE_DISMISS_DELAY = 3000;
    const MESSAGE_DISMISS_DURATION = 220;

    let runtimeMessageContainer = null;
    let activeMessage = null;
    let dismissTimer = 0;
    let removeTimer = 0;
    let activeField = null;
    let activeAnchor = null;
    let lastInvalidForm = null;
    let lastInvalidAt = 0;
    let invalidShownInCycle = false;

    function getDismissDuration() {
        if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            return 0;
        }
        return MESSAGE_DISMISS_DURATION;
    }

    function ensureMessageContainer() {
        if (runtimeMessageContainer && runtimeMessageContainer.isConnected) {
            return runtimeMessageContainer;
        }

        runtimeMessageContainer = document.createElement("div");
        runtimeMessageContainer.className = "messages-wrap";
        runtimeMessageContainer.dataset.runtimeValidation = "true";
        runtimeMessageContainer.setAttribute("aria-live", "polite");
        runtimeMessageContainer.setAttribute("aria-atomic", "true");
        document.body.appendChild(runtimeMessageContainer);
        return runtimeMessageContainer;
    }

    function positionMessageContainer() {
        const container = ensureMessageContainer();
        let top = 20;

        document.querySelectorAll(".messages-wrap").forEach((candidate) => {
            if (candidate === container) {
                return;
            }

            const rect = candidate.getBoundingClientRect();
            if ((rect.width <= 0 && rect.height <= 0) || window.getComputedStyle(candidate).display === "none") {
                return;
            }

            top = Math.max(top, Math.round(rect.bottom + 10));
        });

        container.style.top = `${top}px`;
    }

    function isElement(value) {
        return value instanceof HTMLElement;
    }

    function getLabelTextFromLabel(label) {
        if (!isElement(label)) {
            return "";
        }

        for (const node of label.childNodes) {
            if (node.nodeType === Node.TEXT_NODE) {
                const text = (node.textContent || "").replace(/\s+/g, " ").trim();
                if (text) {
                    return text.replace(/[：:]+$/, "");
                }
            }
        }

        const fallback = (label.getAttribute("aria-label") || label.textContent || "").replace(/\s+/g, " ").trim();
        return fallback.replace(/[：:]+$/, "");
    }

    function resolveFieldLabel(field) {
        if (!isElement(field)) {
            return "";
        }

        const closestLabel = field.closest("label");
        const closestLabelText = getLabelTextFromLabel(closestLabel);
        if (closestLabelText) {
            return closestLabelText;
        }

        if (field.id) {
            const externalLabel = document.querySelector(`label[for="${CSS.escape(field.id)}"]`);
            const externalLabelText = getLabelTextFromLabel(externalLabel);
            if (externalLabelText) {
                return externalLabelText;
            }
        }

        const ariaLabel = (field.getAttribute("aria-label") || "").trim();
        if (ariaLabel) {
            return ariaLabel;
        }

        const placeholder = (field.getAttribute("placeholder") || "").trim();
        if (placeholder) {
            return placeholder;
        }

        return (field.getAttribute("name") || "").trim();
    }

    function resolveAnchor(field, preferredAnchor) {
        if (isElement(preferredAnchor)) {
            return preferredAnchor;
        }

        if (!isElement(field)) {
            return null;
        }

        if (field.classList.contains("ui-picker-native")) {
            return field.closest(".ui-picker")?.querySelector(".ui-picker-trigger") || field;
        }

        const customSelect = field.closest(".custom-select");
        if (customSelect) {
            return customSelect.querySelector(".custom-select-trigger") || field;
        }

        const authInput = field.closest(".auth-portal-input-wrap")?.querySelector("input, textarea, select");
        if (authInput) {
            return authInput;
        }

        return field;
    }

    function isChoiceLikeAnchor(anchor) {
        return !!(anchor && (anchor.classList.contains("custom-select-trigger") || anchor.classList.contains("ui-picker-trigger")));
    }

    function trimBubbleText(text) {
        return (text || "").trim().replace(/。+$/u, "");
    }

    function buildMessage(field, anchor, fallbackMessage) {
        const message = trimBubbleText(fallbackMessage || (field && typeof field.validationMessage === "string" ? field.validationMessage : ""));
        if (message) {
            return message;
        }

        const label = resolveFieldLabel(field);
        if (isChoiceLikeAnchor(anchor)) {
            return label ? `请选择${label}` : "请选择此项";
        }

        if (field && field.hasAttribute && field.hasAttribute("required")) {
            return label ? `请填写${label}` : "请填写此项";
        }

        return label ? `请检查${label}` : "请完善此项";
    }

    function setFieldInvalidState(field, anchor) {
        clearFieldInvalidState(activeField, activeAnchor);

        if (isElement(field)) {
            field.classList.add(FIELD_INVALID_CLASS);
            field.setAttribute("aria-invalid", "true");
        }

        if (isElement(anchor) && anchor !== field) {
            anchor.classList.add(FIELD_INVALID_CLASS);
            anchor.setAttribute("aria-invalid", "true");
        }

        activeField = field;
        activeAnchor = anchor;
    }

    function clearFieldInvalidState(field, anchor) {
        if (isElement(field)) {
            field.classList.remove(FIELD_INVALID_CLASS);
            if (field.checkValidity && field.checkValidity()) {
                field.removeAttribute("aria-invalid");
            }
        }

        if (isElement(anchor) && anchor !== field) {
            anchor.classList.remove(FIELD_INVALID_CLASS);
            if (!field || !field.checkValidity || field.checkValidity()) {
                anchor.removeAttribute("aria-invalid");
            }
        }
    }

    function clearActiveValidation() {
        clearFieldInvalidState(activeField, activeAnchor);
        activeField = null;
        activeAnchor = null;
    }

    function clearMessageTimers() {
        if (dismissTimer) {
            window.clearTimeout(dismissTimer);
            dismissTimer = 0;
        }

        if (removeTimer) {
            window.clearTimeout(removeTimer);
            removeTimer = 0;
        }
    }

    function cleanupMessageContainer() {
        if (runtimeMessageContainer && runtimeMessageContainer.isConnected && runtimeMessageContainer.childElementCount === 0) {
            runtimeMessageContainer.remove();
            runtimeMessageContainer = null;
        }
    }

    function removeActiveMessage(immediate = false) {
        clearMessageTimers();

        if (!activeMessage) {
            cleanupMessageContainer();
            return;
        }

        const message = activeMessage;
        activeMessage = null;

        const finishRemoval = () => {
            if (message.isConnected) {
                message.remove();
            }
            cleanupMessageContainer();
        };

        const duration = getDismissDuration();
        if (immediate || duration === 0) {
            finishRemoval();
            return;
        }

        message.style.setProperty("--msg-dismiss-duration", `${duration}ms`);
        message.classList.remove("is-visible");
        message.classList.add("is-closing");
        removeTimer = window.setTimeout(finishRemoval, duration);
    }

    function composeMessageText(title, message) {
        const resolvedMessage = trimBubbleText(message);
        const resolvedTitle = trimBubbleText(title);

        if (!resolvedMessage) {
            return resolvedTitle || "请完善此项";
        }

        if (!resolvedTitle || resolvedTitle === "请完善此项") {
            return resolvedMessage;
        }

        return `${resolvedTitle}：${resolvedMessage}`;
    }

    function showTopMessage(text, type = "error") {
        removeActiveMessage(true);

        const container = ensureMessageContainer();
        positionMessageContainer();

        const alert = document.createElement("div");
        alert.className = `msg ${type === "success" ? "success" : "error"}`;
        alert.setAttribute("role", "alert");
        alert.textContent = trimBubbleText(text);
        alert.style.setProperty("--msg-dismiss-duration", `${getDismissDuration()}ms`);
        alert.addEventListener("click", () => {
            hideBubble();
        });

        container.appendChild(alert);
        activeMessage = alert;
        window.setTimeout(() => {
            alert.classList.add("is-visible");
        }, 10);
        dismissTimer = window.setTimeout(() => {
            removeActiveMessage(false);
        }, MESSAGE_DISMISS_DELAY);
    }

    function hideBubble() {
        removeActiveMessage(true);
        clearActiveValidation();
    }

    function showBubbleForField(field, options = {}) {
        const anchor = resolveAnchor(field, options.anchor);
        const message = buildMessage(field, anchor, options.message);
        const title = (options.title || "请完善此项").trim();

        setFieldInvalidState(field, anchor);
        showTopMessage(composeMessageText(title, message));
    }

    function shouldStartNewInvalidCycle(form, now) {
        return form !== lastInvalidForm || now - lastInvalidAt > CYCLE_WINDOW_MS;
    }

    function handleInvalid(event) {
        const field = event.target;
        if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement || field instanceof HTMLTextAreaElement)) {
            return;
        }

        event.preventDefault();
        const now = window.performance && typeof window.performance.now === "function"
            ? window.performance.now()
            : Date.now();
        const form = field.form || null;

        if (shouldStartNewInvalidCycle(form, now)) {
            invalidShownInCycle = false;
        }

        lastInvalidForm = form;
        lastInvalidAt = now;

        if (invalidShownInCycle) {
            return;
        }

        invalidShownInCycle = true;
        showBubbleForField(field);
    }

    function maybeClearForField(field) {
        if (!isElement(field)) {
            return;
        }

        if (field === activeField) {
            if (!field.checkValidity || field.checkValidity()) {
                hideBubble();
            }
            return;
        }

        const anchor = resolveAnchor(field);
        clearFieldInvalidState(field, anchor);
    }

    function bindClearEvents() {
        ["input", "change"].forEach((eventName) => {
            document.addEventListener(eventName, (event) => {
                maybeClearForField(event.target);
            }, true);
        });

        document.addEventListener("focusin", (event) => {
            if ((event.target === activeField || event.target === activeAnchor) && activeMessage) {
                positionMessageContainer();
            }
        }, true);

        window.addEventListener("resize", () => {
            if (activeMessage) {
                positionMessageContainer();
            }
        });
    }

    function bindCustomEvents() {
        document.addEventListener("validationbubble:show", (event) => {
            const detail = event.detail || {};
            if (!isElement(detail.field) && !isElement(detail.anchor)) {
                return;
            }
            showBubbleForField(detail.field || detail.anchor, detail);
        });

        document.addEventListener("validationbubble:clear", (event) => {
            const detail = event.detail || {};
            const field = detail.field || detail.anchor;
            if (!field) {
                hideBubble();
                return;
            }
            maybeClearForField(field);
        });
    }

    document.addEventListener("invalid", handleInvalid, true);
    bindClearEvents();
    bindCustomEvents();

    window.ValidationBubble = {
        showForField: (field, options = {}) => {
            if (!isElement(field)) {
                return;
            }
            showBubbleForField(field, options);
        },
        clearForField: (field) => {
            maybeClearForField(field);
        },
        hide: () => {
            hideBubble();
        },
        showMessage: (message, type = "error") => {
            if (!message) {
                return;
            }
            clearActiveValidation();
            showTopMessage(message, type);
        }
    };
})();
