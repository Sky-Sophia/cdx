document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-compose-modal]");
    if (!modal) {
        return;
    }

    const openButtons = Array.prototype.slice.call(document.querySelectorAll("[data-compose-open]"));
    const closeButtons = Array.prototype.slice.call(modal.querySelectorAll("[data-compose-close]"));
    const form = modal.querySelector("[data-compose-form]");
    const targetTypeField = modal.querySelector("[data-compose-target-type]");
    const receiverLabel = modal.querySelector("[data-compose-receiver-label]");
    const receiverContainer = modal.querySelector("[data-compose-receiver-container]");
    const contentField = modal.querySelector("[name='content']");
    const msgTypeField = modal.querySelector("[name='msg_type']");
    const submitButton = form ? form.querySelector("[type='submit']") : null;
    const buildingOptions = readOptions("[data-compose-building-source]");
    const departmentOptions = readOptions("[data-compose-department-source]");
    let lastActiveElement = null;
    let hideTimer = 0;
    let sending = false;

    if (!form || !targetTypeField || !receiverLabel || !receiverContainer || !contentField || !msgTypeField) {
        return;
    }

    function readOptions(selector) {
        return Array.prototype.map.call(modal.querySelectorAll(selector + " [data-value]"), function (node) {
            return {
                value: (node.getAttribute("data-value") || "").trim(),
                label: (node.getAttribute("data-label") || node.textContent || "").trim()
            };
        }).filter(function (item) {
            return item.value && item.label;
        });
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function showMessage(message, type) {
        if (window.ValidationBubble && typeof window.ValidationBubble.showMessage === "function") {
            window.ValidationBubble.showMessage(message, type || "error");
        }
    }

    function showFieldMessage(field, message) {
        if (window.ValidationBubble && typeof window.ValidationBubble.showForField === "function" && field) {
            window.ValidationBubble.showForField(field, { message: message });
            return;
        }
        showMessage(message, "error");
    }

    function supportsStableScrollbarGutter() {
        return typeof CSS !== "undefined"
            && typeof CSS.supports === "function"
            && CSS.supports("scrollbar-gutter: stable");
    }

    function lockBodyScroll() {
        const body = document.body;
        const root = document.documentElement;
        if (body.classList.contains("compose-modal-open")) {
            return;
        }

        if (!supportsStableScrollbarGutter()) {
            body.dataset.composeModalOriginalPaddingRight = body.style.paddingRight || "";
            const scrollbarWidth = window.innerWidth - root.clientWidth;
            if (scrollbarWidth > 0) {
                const computedPaddingRight = Number.parseFloat(window.getComputedStyle(body).paddingRight) || 0;
                body.style.paddingRight = `${computedPaddingRight + scrollbarWidth}px`;
            }
        }

        root.classList.add("compose-modal-open");
        body.classList.add("compose-modal-open");
    }

    function unlockBodyScroll() {
        const body = document.body;
        const root = document.documentElement;
        root.classList.remove("compose-modal-open");
        body.classList.remove("compose-modal-open");

        if (Object.prototype.hasOwnProperty.call(body.dataset, "composeModalOriginalPaddingRight")) {
            body.style.paddingRight = body.dataset.composeModalOriginalPaddingRight;
            delete body.dataset.composeModalOriginalPaddingRight;
        } else {
            body.style.removeProperty("padding-right");
        }
    }

    function setSendingState(nextSending) {
        sending = !!nextSending;
        if (!submitButton) {
            return;
        }
        submitButton.disabled = sending;
        submitButton.textContent = sending ? "发送中..." : "确认发送";
    }

    function buildCustomSelectHtml(options, emptyText) {
        if (!options.length) {
            return `<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="${escapeHtml(emptyText)}" readonly>`;
        }

        const first = options[0];
        return [
            '<div class="custom-select">',
            `    <input type="hidden" id="composeReceiver" name="receiver" value="${escapeHtml(first.value)}">`,
            '    <button type="button" class="custom-select-trigger" aria-expanded="false">',
            `        <span class="custom-select-trigger-text">${escapeHtml(first.label)}</span>`,
            '        <span class="custom-select-arrow" aria-hidden="true"></span>',
            "    </button>",
            '    <ul class="custom-select-panel">',
            options.map(function (item, index) {
                return `        <li class="custom-select-option${index === 0 ? " is-selected" : ""}" data-value="${escapeHtml(item.value)}" data-label="${escapeHtml(item.label)}">${escapeHtml(item.label)}</li>`;
            }).join(""),
            "    </ul>",
            "</div>"
        ].join("");
    }

    function resolveReceiverConfig(type) {
        switch (type) {
            case "all":
                return {
                    label: "接收范围",
                    html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="全体系统用户" readonly>'
                };
            case "department":
                return {
                    label: "选择部门",
                    html: buildCustomSelectHtml(departmentOptions, "暂无可选部门")
                };
            case "building":
                return {
                    label: "选择楼栋",
                    html: buildCustomSelectHtml(buildingOptions, "暂无可选楼栋")
                };
            case "due_bill":
                return {
                    label: "接收范围",
                    html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="未缴费关联用户" readonly>'
                };
            case "work_order_done":
                return {
                    label: "接收范围",
                    html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="已完工单关联用户" readonly>'
                };
            case "single":
            default:
                return {
                    label: "接收人ID",
                    html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" placeholder="请输入用户ID或用户名" autocomplete="off">'
                };
        }
    }

    function closeNoticeDropdown() {
        const api = window.notificationDropdownApi;
        if (api && typeof api.close === "function") {
            api.close();
        }
    }

    function isOpen() {
        return !modal.hidden && modal.classList.contains("is-open");
    }

    function getReceiverField() {
        return form.querySelector("[name='receiver']");
    }

    function getPrimaryTrigger() {
        return form.querySelector("[data-compose-primary-trigger]");
    }

    function syncCustomSelect(wrapper) {
        if (!wrapper) {
            return;
        }

        const hiddenInput = wrapper.querySelector("input[type='hidden']");
        const triggerText = wrapper.querySelector(".custom-select-trigger-text");
        const options = Array.prototype.slice.call(wrapper.querySelectorAll(".custom-select-option"));

        if (!hiddenInput || !triggerText || !options.length) {
            return;
        }

        let matchedOption = null;
        options.forEach(function (option) {
            const isSelected = option.getAttribute("data-value") === hiddenInput.value;
            option.classList.toggle("is-selected", isSelected);
            if (isSelected) {
                matchedOption = option;
            }
        });

        if (matchedOption) {
            triggerText.textContent = matchedOption.getAttribute("data-label") || matchedOption.textContent.trim();
        }
    }

    function renderReceiverField(type) {
        const config = resolveReceiverConfig(type);
        receiverLabel.textContent = config.label;
        receiverContainer.innerHTML = config.html;
    }

    function resetForm() {
        form.reset();
        Array.prototype.forEach.call(
            form.querySelectorAll(".compose-modal-body > .compose-form-item > .custom-select"),
            syncCustomSelect
        );
        renderReceiverField(targetTypeField.value || "single");
        contentField.value = "";
        setSendingState(false);
    }

    function setExpanded(expanded) {
        openButtons.forEach(function (button) {
            button.setAttribute("aria-expanded", expanded ? "true" : "false");
        });
    }

    function openModal(trigger) {
        if (hideTimer) {
            window.clearTimeout(hideTimer);
            hideTimer = 0;
        }

        lastActiveElement = trigger || document.activeElement;
        closeNoticeDropdown();
        resetForm();
        modal.hidden = false;
        modal.setAttribute("aria-hidden", "false");
        lockBodyScroll();
        setExpanded(true);

        window.requestAnimationFrame(function () {
            modal.classList.add("is-open");
            const firstField = getPrimaryTrigger();
            if (firstField) {
                firstField.focus();
            }
        });
    }

    function closeModal() {
        if (!isOpen()) {
            modal.hidden = true;
            modal.setAttribute("aria-hidden", "true");
            setSendingState(false);
            return;
        }

        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        unlockBodyScroll();
        setExpanded(false);
        setSendingState(false);

        hideTimer = window.setTimeout(function () {
            modal.hidden = true;
            hideTimer = 0;
        }, 180);

        if (lastActiveElement && typeof lastActiveElement.focus === "function") {
            lastActiveElement.focus();
        }
    }

    function sendPayload(payload) {
        setSendingState(true);
        const detail = {
            handled: false,
            payload: payload,
            onSuccess: function (response) {
                const dispatchCount = response && Number.isFinite(Number(response.dispatchCount))
                    ? Number(response.dispatchCount)
                    : 0;
                showMessage(dispatchCount > 0 ? `发送成功，已投递 ${dispatchCount} 人` : "发送成功！", "success");
                setSendingState(false);
                closeModal();
            },
            onError: function (message) {
                setSendingState(false);
                showMessage(message || "通知发送失败，请稍后重试。", "error");
            }
        };

        window.dispatchEvent(new CustomEvent("compose-notice:submit", {
            detail: detail
        }));

        if (!detail.handled) {
            detail.onError("通知通道未就绪，请刷新页面后重试。");
        }
    }

    openButtons.forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            openModal(button);
        });
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            closeModal();
        });
    });

    targetTypeField.addEventListener("change", function () {
        renderReceiverField(targetTypeField.value);
    });

    modal.addEventListener("click", function (event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !modal.hidden) {
            closeModal();
        }
    });

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        if (sending) {
            return;
        }

        const targetType = targetTypeField.value;
        const receiverField = getReceiverField();
        const receiverValue = receiverField ? receiverField.value.trim() : "";
        const contentValue = contentField.value.trim();

        if (targetType === "building" && !buildingOptions.length) {
            showMessage("当前没有可发送的楼栋数据。", "error");
            return;
        }

        if (targetType === "department" && !departmentOptions.length) {
            showMessage("当前没有可发送的部门数据。", "error");
            return;
        }

        if (targetType === "single" && !receiverValue) {
            showFieldMessage(receiverField, "请输入接收人ID或用户名");
            if (receiverField) {
                receiverField.focus();
            }
            return;
        }

        if (!contentValue) {
            showFieldMessage(contentField, "请输入消息内容");
            contentField.focus();
            return;
        }

        sendPayload({
            msgType: msgTypeField.value,
            content: contentValue,
            targetType: targetType,
            receiver: receiverValue
        });
    });

    renderReceiverField(targetTypeField.value || "single");
});
