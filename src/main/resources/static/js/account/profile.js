(() => {
    "use strict";

    function setupProfileHistoryModal() {
        const notificationCommon = window.NotificationCommon;
        const modal = document.querySelector("[data-history-modal]");
        const openButton = document.querySelector("[data-history-open]");
        const closeButtons = document.querySelectorAll("[data-history-close]");
        const filterButtons = document.querySelectorAll("[data-history-filter]");
        const listElement = document.querySelector("[data-history-list]");
        const emptyState = document.querySelector("[data-history-empty]");
        const deleteAllButton = document.querySelector("[data-history-delete-all]");

        if (!notificationCommon || !modal || !openButton || !listElement || !emptyState) {
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

        function parseItemElement(element) {
            const typeMeta = notificationCommon.resolveTypeMeta(
                element.querySelector(".profile-history-type")?.textContent || element.dataset.historyType || "通知"
            );
            return {
                id: Number(element.dataset.messageId || 0),
                msgType: typeMeta.label,
                historyType: element.dataset.historyType || typeMeta.historyType,
                sender: element.querySelector(".profile-history-item-title")?.textContent?.trim() || "系统消息",
                content: element.querySelector(".profile-history-item-content")?.textContent?.trim() || "",
                sendTime: element.dataset.sendTime || "",
                read: element.dataset.historyRead === "true"
            };
        }

        function normalizeIncomingItem(item) {
            const normalized = notificationCommon.normalizeItem(item);
            const typeMeta = notificationCommon.resolveTypeMeta(normalized.msgType);
            return {
                ...normalized,
                msgType: typeMeta.label,
                historyType: typeMeta.historyType,
                sender: String(normalized.sender || "").trim() || "系统消息",
                content: String(normalized.content || "").trim()
            };
        }

        function sortItems(items) {
            return items.slice().sort((left, right) => {
                const leftTime = notificationCommon.parseDate(left.sendTime);
                const rightTime = notificationCommon.parseDate(right.sendTime);
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
                notificationCommon.showMessage("历史消息已全部删除", "success");
            }
        }

        function getEmptyStateText(filteredItems) {
            if (filteredItems.length > 0) {
                return "";
            }
            if (!state.hasSynced && state.items.length === 0) {
                return "正在加载消息...";
            }
            if (state.activeFilter !== "all" && state.items.length > 0) {
                return "当前筛选下暂无消息";
            }
            return "暂无消息";
        }

        function createItemElement(item) {
            const typeMeta = notificationCommon.resolveTypeMeta(item.msgType);

            const button = document.createElement("button");
            button.type = "button";
            button.className = "profile-history-item";
            button.dataset.historyType = item.historyType || typeMeta.historyType;
            button.dataset.historyRead = item.read ? "true" : "false";
            button.dataset.messageId = String(item.id || 0);
            button.dataset.sendTime = item.sendTime || "";

            const iconWrap = document.createElement("div");
            iconWrap.className = "profile-history-item-icon";
            const icon = document.createElement("img");
            icon.src = typeMeta.iconPath;
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
            typeTag.dataset.tone = item.historyType || typeMeta.historyType;
            typeTag.textContent = typeMeta.label;

            const title = document.createElement("p");
            title.className = "profile-history-item-title";
            title.textContent = item.sender || "系统消息";

            const time = document.createElement("span");
            time.className = "profile-history-item-time";
            time.textContent = notificationCommon.formatRelativeTime(item.sendTime, {
                includeTimeForSameDay: false,
                includeTimeForYesterday: false
            });

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
                const detailItem = { ...item };
                if (!item.read) {
                    window.dispatchEvent(new CustomEvent("notification:read", {
                        detail: { id: item.id }
                    }));
                    item.read = true;
                    render();
                }
                notificationCommon.openDetail(detailItem, { title: "通知详情" });
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

            const notificationCenterApi = window.notificationCenterApi;
            if (!notificationCenterApi || typeof notificationCenterApi.deleteAll !== "function") {
                notificationCommon.showMessage("通知中心尚未准备完成，请稍后重试", "error");
                return;
            }

            state.deletingAll = true;
            updateDeleteAllButton();
            clearDeleteAllTimer();
            state.deleteAllTimer = window.setTimeout(() => {
                finishDeleteAll(false);
                notificationCommon.showMessage("删除失败，请稍后重试", "error");
            }, 5000);

            notificationCenterApi.deleteAll()
                .then((success) => {
                    if (!success) {
                        finishDeleteAll(false);
                        notificationCommon.showMessage("删除失败，请稍后重试", "error");
                    }
                })
                .catch((error) => {
                    window.console.error("删除历史消息失败", error);
                    finishDeleteAll(false);
                    notificationCommon.showMessage("删除失败，请稍后重试", "error");
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
            if (window.notificationDetailModal
                && typeof window.notificationDetailModal.isOpen === "function"
                && window.notificationDetailModal.isOpen()) {
                return;
            }
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
                notificationCommon.showMessage(detail.message || "删除失败，请稍后重试", "error");
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
