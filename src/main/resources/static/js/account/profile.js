(() => {
    "use strict";

    function setupProfileHistoryModal() {
        const modal = document.querySelector("[data-history-modal]");
        const openButton = document.querySelector("[data-history-open]");
        const closeButtons = document.querySelectorAll("[data-history-close]");
        const filterButtons = document.querySelectorAll("[data-history-filter]");
        const listElement = document.querySelector("[data-history-list]");
        const emptyState = document.querySelector("[data-history-empty]");
        const deleteAllButton = document.querySelector("[data-history-delete-all]");

        if (!modal || !openButton || !listElement || !emptyState) {
            return;
        }

        const initialElements = Array.from(listElement.querySelectorAll(".profile-history-item"));
        const state = {
            activeFilter: "all",
            items: initialElements.map(parseItemElement),
            hasSynced: initialElements.length > 0,
            deletingAll: false,
            deleteAllTimer: 0
        };

        initialElements.forEach((element) => element.remove());

        function parseDate(value) {
            if (!value) {
                return null;
            }
            const parsed = new Date(value);
            return Number.isNaN(parsed.getTime()) ? null : parsed;
        }

        function startOfDay(date) {
            return new Date(date.getFullYear(), date.getMonth(), date.getDate());
        }

        function formatTimeLabel(value) {
            const date = parseDate(value);
            if (!date) {
                return "刚刚";
            }

            const now = new Date();
            const msPerDay = 24 * 60 * 60 * 1000;
            const dayDiff = Math.floor((startOfDay(now).getTime() - startOfDay(date).getTime()) / msPerDay);
            const hours = String(date.getHours()).padStart(2, "0");
            const minutes = String(date.getMinutes()).padStart(2, "0");

            if (dayDiff <= 0) {
                return `${hours}:${minutes}`;
            }
            if (dayDiff === 1) {
                return "昨天";
            }
            if (dayDiff < 7) {
                return `${dayDiff}天前`;
            }
            return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
        }

        function resolveHistoryType(msgType) {
            const typeLabel = String(msgType || "").trim() || "通知";
            switch (typeLabel) {
                case "公告":
                    return { type: "announce", iconPath: "/icons/speaker.svg", label: "公告" };
                case "提醒":
                    return { type: "reminder", iconPath: "/icons/device.svg", label: "提醒" };
                case "预警":
                    return { type: "warning", iconPath: "/icons/shield.svg", label: "预警" };
                default:
                    return { type: "notice", iconPath: "/icons/bell.svg", label: typeLabel || "通知" };
            }
        }

        function parseItemElement(element) {
            const typeInfo = resolveHistoryType(element.querySelector(".profile-history-type")?.textContent || element.dataset.historyType || "通知");
            return {
                id: Number(element.dataset.messageId || 0),
                msgType: typeInfo.label,
                historyType: element.dataset.historyType || typeInfo.type,
                sender: element.querySelector(".profile-history-item-title")?.textContent?.trim() || "系统消息",
                content: element.querySelector(".profile-history-item-content")?.textContent?.trim() || "",
                sendTime: element.dataset.sendTime || "",
                read: element.dataset.historyRead === "true"
            };
        }

        function normalizeIncomingItem(item) {
            const typeInfo = resolveHistoryType(item.msgType);
            return {
                id: Number(item.id || 0),
                msgType: typeInfo.label,
                historyType: typeInfo.type,
                sender: String(item.sender || "").trim() || "系统消息",
                content: String(item.content || "").trim(),
                sendTime: item.sendTime || "",
                read: !!item.read
            };
        }

        function sortItems(items) {
            return items.slice().sort((left, right) => {
                const leftTime = parseDate(left.sendTime);
                const rightTime = parseDate(right.sendTime);
                const leftValue = leftTime ? leftTime.getTime() : 0;
                const rightValue = rightTime ? rightTime.getTime() : 0;
                if (leftValue !== rightValue) {
                    return rightValue - leftValue;
                }
                return Number(right.id || 0) - Number(left.id || 0);
            });
        }

        function updateDeleteAllButton() {
            if (!deleteAllButton) {
                return;
            }
            deleteAllButton.disabled = state.items.length === 0 || state.deletingAll;
        }

        function showMessage(message, type) {
            if (window.ValidationBubble && typeof window.ValidationBubble.showMessage === "function") {
                window.ValidationBubble.showMessage(message, type || "error");
                return;
            }
            window.console[type === "success" ? "info" : "warn"](message);
        }

        function clearDeleteAllTimer() {
            if (state.deleteAllTimer) {
                window.clearTimeout(state.deleteAllTimer);
                state.deleteAllTimer = 0;
            }
        }

        function finishDeleteAll(success) {
            const wasDeleting = state.deletingAll;
            state.deletingAll = false;
            clearDeleteAllTimer();
            updateDeleteAllButton();
            if (!wasDeleting) {
                return;
            }
            if (success) {
                showMessage("\u5386\u53f2\u6d88\u606f\u5df2\u5168\u90e8\u5220\u9664", "success");
            }
        }

        function getEmptyStateText(filteredItems) {
            if (filteredItems.length > 0) {
                return "";
            }
            if (!state.hasSynced && state.items.length === 0) {
                return "\u6b63\u5728\u52a0\u8f7d\u6d88\u606f...";
            }
            if (state.activeFilter !== "all" && state.items.length > 0) {
                return "\u5f53\u524d\u7b5b\u9009\u4e0b\u6682\u65e0\u6d88\u606f";
            }
            return "\u6682\u65e0\u6d88\u606f";
        }

        function createItemElement(item) {
            const typeInfo = resolveHistoryType(item.msgType);

            const button = document.createElement("button");
            button.type = "button";
            button.className = "profile-history-item";
            button.dataset.historyType = item.historyType || typeInfo.type;
            button.dataset.historyRead = item.read ? "true" : "false";
            button.dataset.messageId = String(item.id || 0);
            button.dataset.sendTime = item.sendTime || "";

            const iconWrap = document.createElement("div");
            iconWrap.className = "profile-history-item-icon";
            const icon = document.createElement("img");
            icon.src = typeInfo.iconPath;
            icon.alt = "";
            iconWrap.appendChild(icon);

            const body = document.createElement("div");
            body.className = "profile-history-item-body";

            const meta = document.createElement("div");
            meta.className = "profile-history-item-meta";

            const heading = document.createElement("div");
            heading.className = "profile-history-item-heading";

            const typeTag = document.createElement("span");
            typeTag.className = "profile-history-type";
            typeTag.dataset.tone = item.historyType || typeInfo.type;
            typeTag.textContent = typeInfo.label;

            const title = document.createElement("p");
            title.className = "profile-history-item-title";
            title.textContent = item.sender || "系统消息";

            const time = document.createElement("span");
            time.className = "profile-history-item-time";
            time.textContent = formatTimeLabel(item.sendTime);

            const content = document.createElement("p");
            content.className = "profile-history-item-content";
            content.textContent = item.content || "暂无消息内容";

            heading.appendChild(typeTag);
            heading.appendChild(title);
            meta.appendChild(heading);
            meta.appendChild(time);
            body.appendChild(meta);
            body.appendChild(content);
            button.appendChild(iconWrap);
            button.appendChild(body);

            button.addEventListener("click", () => {
                if (!item.read) {
                    window.dispatchEvent(new CustomEvent("notification:read", {
                        detail: { id: item.id }
                    }));
                    item.read = true;
                    render();
                }
            });

            return button;
        }

        function render() {
            const sortedItems = sortItems(state.items);
            const filteredItems = sortedItems.filter((item) => state.activeFilter === "all" || item.historyType === state.activeFilter);

            listElement.querySelectorAll(".profile-history-item").forEach((item) => item.remove());
            filteredItems.forEach((item) => {
                listElement.insertBefore(createItemElement(item), emptyState);
            });

            emptyState.textContent = getEmptyStateText(filteredItems);
            emptyState.hidden = filteredItems.length > 0;
            updateDeleteAllButton();
        }

        function setItems(items) {
            state.hasSynced = true;
            state.items = Array.isArray(items) ? items.map(normalizeIncomingItem) : [];
            render();
        }

        function upsertItem(item) {
            const normalized = normalizeIncomingItem(item);
            const index = state.items.findIndex((existing) => String(existing.id) === String(normalized.id));
            if (index >= 0) {
                state.items[index] = normalized;
            } else {
                state.items.unshift(normalized);
            }
            render();
        }

        function removeItem(id) {
            state.items = state.items.filter((item) => String(item.id) !== String(id));
            render();
        }

        function removeItems(ids) {
            const idSet = new Set((ids || []).map((id) => String(id)));
            state.items = idSet.size === 0
                ? []
                : state.items.filter((item) => !idSet.has(String(item.id)));
            render();
        }

        function openModal() {
            modal.hidden = false;
            modal.setAttribute("aria-hidden", "false");
            openButton.setAttribute("aria-expanded", "true");
            document.body.classList.add("profile-history-modal-open");
            listElement.scrollTop = 0;
            render();
        }

        function closeModal() {
            modal.hidden = true;
            modal.setAttribute("aria-hidden", "true");
            openButton.setAttribute("aria-expanded", "false");
            document.body.classList.remove("profile-history-modal-open");
        }

        function deleteAllMessages() {
            if (!deleteAllButton || state.items.length === 0 || state.deletingAll) {
                return;
            }
            if (deleteAllButton.getAttribute("data-confirm-approved") !== "true") {
                return;
            }

            state.deletingAll = true;
            updateDeleteAllButton();
            clearDeleteAllTimer();
            state.deleteAllTimer = window.setTimeout(() => {
                finishDeleteAll(false);
                showMessage("\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", "error");
            }, 5000);

            window.fetch("/api/notifications", {
                method: "DELETE",
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                },
                credentials: "same-origin"
            })
                .then((response) => {
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}`);
                    }
                    return response.json();
                })
                .then((result) => {
                    window.dispatchEvent(new CustomEvent("notification:data-deleted-all", {
                        detail: {
                            ids: result.ids || [],
                            unreadCount: Number(result.unreadCount || 0)
                        }
                    }));
                })
                .catch((error) => {
                    window.console.error("删除历史消息失败", error);
                    if (window.notificationCenterApi
                        && typeof window.notificationCenterApi.deleteAllViaSocket === "function"
                        && window.notificationCenterApi.deleteAllViaSocket()) {
                        return;
                    }
                    finishDeleteAll(false);
                    showMessage("\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", "error");
                });
        }

        openButton.addEventListener("click", openModal);

        closeButtons.forEach((button) => {
            button.addEventListener("click", closeModal);
        });

        modal.addEventListener("click", (event) => {
            if (event.target === modal) {
                closeModal();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !modal.hidden) {
                closeModal();
            }
        });

        filterButtons.forEach((button) => {
            button.addEventListener("click", () => {
                state.activeFilter = button.dataset.historyFilter || "all";
                filterButtons.forEach((current) => {
                    current.classList.toggle("is-active", current === button);
                });
                listElement.scrollTop = 0;
                render();
            });
        });

        if (deleteAllButton) {
            deleteAllButton.addEventListener("click", deleteAllMessages);
        }

        window.addEventListener("notification:data-sync", (event) => {
            const detail = event.detail || {};
            if (Array.isArray(detail.items)) {
                setItems(detail.items);
            }
        });

        window.addEventListener("notification:data-created", (event) => {
            const detail = event.detail || {};
            if (detail.item) {
                upsertItem(detail.item);
            }
        });

        window.addEventListener("notification:data-updated", (event) => {
            const detail = event.detail || {};
            if (detail.item) {
                upsertItem(detail.item);
            }
        });

        window.addEventListener("notification:data-read-all", (event) => {
            const detail = event.detail || {};
            const idSet = new Set((detail.ids || []).map((id) => String(id)));
            state.items = state.items.map((item) => {
                if (idSet.size === 0 || idSet.has(String(item.id))) {
                    return { ...item, read: true };
                }
                return item;
            });
            render();
        });

        window.addEventListener("notification:data-deleted", (event) => {
            const detail = event.detail || {};
            if (detail.id) {
                removeItem(detail.id);
            }
        });

        window.addEventListener("notification:data-deleted-all", (event) => {
            const detail = event.detail || {};
            finishDeleteAll(true);
            removeItems(detail.ids || []);
        });

        window.addEventListener("notification:action-error", (event) => {
            const detail = event.detail || {};
            if (detail.action === "DELETE_ALL") {
                finishDeleteAll(false);
                showMessage(detail.message || "\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", "error");
            }
        });

        render();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", setupProfileHistoryModal, { once: true });
    } else {
        setupProfileHistoryModal();
    }
})();
