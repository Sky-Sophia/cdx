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
    let lastActiveElement = null;
    let hideTimer = 0;

    if (!form || !targetTypeField || !receiverLabel || !receiverContainer || !contentField || !msgTypeField) {
        return;
    }

    const receiverTemplates = {
        all: {
            label: "接收范围",
            html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="全体系统用户" readonly>'
        },
        single: {
            label: "接收人ID",
            html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" placeholder="请输入用户ID或用户名" autocomplete="off">'
        },
        department: {
            label: "选择部门",
            html: [
                '<select id="composeReceiver" name="receiver" class="compose-form-select">',
                '<option value="行政部">行政部</option>',
                '<option value="财务部">财务部</option>',
                '<option value="工程部">工程部</option>',
                '<option value="安保部">安保部</option>',
                "</select>"
            ].join("")
        },
        building: {
            label: "选择楼栋",
            html: [
                '<select id="composeReceiver" name="receiver" class="compose-form-select">',
                '<option value="1">1号楼</option>',
                '<option value="2">2号楼</option>',
                '<option value="3">3号楼</option>',
                '<option value="5">5号楼</option>',
                '<option value="6">6号楼</option>',
                "</select>"
            ].join("")
        },
        due_bill: {
            label: "接收范围",
            html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="未缴费关联用户" readonly>'
        },
        work_order_done: {
            label: "接收范围",
            html: '<input type="text" id="composeReceiver" name="receiver" class="compose-form-input" value="已完工单关联用户" readonly>'
        }
    };

    function ensureOption(select, value, label) {
        if (!select.querySelector(`option[value="${value}"]`)) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            select.appendChild(option);
        }
    }

    function removeOption(select, value) {
        const option = select.querySelector(`option[value="${value}"]`);
        if (option) {
            option.remove();
        }
    }

    function normalizeSelectOptions() {
        removeOption(msgTypeField, "due_bill");
        removeOption(msgTypeField, "work_order_done");
        ensureOption(targetTypeField, "due_bill", "发给未缴费");
        ensureOption(targetTypeField, "work_order_done", "发给工单完成");
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

    function renderReceiverField(type) {
        const config = receiverTemplates[type] || receiverTemplates.single;
        receiverLabel.textContent = config.label;
        receiverContainer.innerHTML = config.html;
    }

    function resetForm() {
        form.reset();
        renderReceiverField(targetTypeField.value || "single");
        contentField.value = "";
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
        document.body.classList.add("compose-modal-open");
        setExpanded(true);

        window.requestAnimationFrame(function () {
            modal.classList.add("is-open");
            const firstField = form.querySelector("[name='msg_type']");
            if (firstField) {
                firstField.focus();
            }
        });
    }

    function closeModal() {
        if (!isOpen()) {
            modal.hidden = true;
            modal.setAttribute("aria-hidden", "true");
            return;
        }

        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("compose-modal-open");
        setExpanded(false);

        hideTimer = window.setTimeout(function () {
            modal.hidden = true;
            hideTimer = 0;
        }, 180);

        if (lastActiveElement && typeof lastActiveElement.focus === "function") {
            lastActiveElement.focus();
        }
    }

    function sendPayload(payload) {
        const detail = {
            handled: false,
            payload: payload,
            onSuccess: function () {
                window.alert("发送成功！");
                closeModal();
            },
            onError: function (message) {
                window.alert(message || "通知发送失败，请稍后重试。");
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

        const targetType = targetTypeField.value;
        const receiverField = getReceiverField();
        const receiverValue = receiverField ? receiverField.value.trim() : "";
        const contentValue = contentField.value.trim();

        if (targetType === "single" && !receiverValue) {
            window.alert("请输入接收人ID或用户名！");
            if (receiverField) {
                receiverField.focus();
            }
            return;
        }

        if (!contentValue) {
            window.alert("请输入消息内容！");
            contentField.focus();
            return;
        }

        sendPayload({
            msgType: form.elements.msg_type.value,
            content: contentValue,
            targetType: targetType,
            receiver: receiverValue
        });
    });

    normalizeSelectOptions();
    renderReceiverField(targetTypeField.value || "single");
});
