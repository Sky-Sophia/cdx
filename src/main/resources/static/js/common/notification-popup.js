document.addEventListener("DOMContentLoaded", function () {
    const notificationCommon = window.NotificationCommon;
    const dropdown = document.querySelector("[data-notification-dropdown]");
    if (!dropdown || !notificationCommon) {
        return;
    }

    const toggleButton = dropdown.querySelector("[data-notice-toggle]");
    const panel = dropdown.querySelector("[data-notice-panel]");
    const list = dropdown.querySelector("[data-notice-list]");
    const empty = dropdown.querySelector("[data-notice-empty]");
    const readAllButton = dropdown.querySelector("[data-notice-read-all]");
    const badge = dropdown.querySelector("[data-notice-badge]");

    if (!toggleButton || !panel || !list || !empty || !readAllButton || !badge) {
        return;
    }

    const state = {
        items: [],
        unreadCount: 0,
        emptyText: empty.textContent || "暂无最近通知"
    };

    function isUnread(item) {
        return !item.read;
    }

    function sortItems(items) {
        return items.slice().sort(function (left, right) {
            const unreadRankDiff = Number(left.read) - Number(right.read);
            if (unreadRankDiff !== 0) {
                return unreadRankDiff;
            }

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

    function countUnread(items) {
        return items.reduce(function (count, item) {
            return count + (isUnread(item) ? 1 : 0);
        }, 0);
    }

    function getVisibleItems() {
        return sortItems(state.items).filter(function (item) {
            return !item.popupHidden;
        });
    }

    function createNoticeElement(item) {
        const typeMeta = notificationCommon.resolveTypeMeta(item.msgType);

        const button = document.createElement("button");
        button.type = "button";
        button.className = `notice-item${isUnread(item) ? " unread" : ""}`;
        button.dataset.noticeItem = "true";
        button.dataset.noticeId = String(item.id);
        button.dataset.noticeType = typeMeta.label;
        button.title = item.content || "";

        const meta = document.createElement("span");
        meta.className = "notice-meta";

        const typeTag = document.createElement("span");
        typeTag.className = "notice-type";
        typeTag.dataset.tone = typeMeta.tone;
        typeTag.textContent = typeMeta.label;

        const time = document.createElement("span");
        time.className = "notice-time";
        time.textContent = notificationCommon.formatRelativeTime(item.sendTime);
        meta.appendChild(typeTag);
        meta.appendChild(time);

        const content = document.createElement("span");
        content.className = "notice-content";
        content.textContent = item.sender
            ? `${item.sender}：${item.content || "暂无消息内容"}`
            : (item.content || "暂无消息内容");

        const deleteBtn = document.createElement("span");
        deleteBtn.className = "notice-delete";
        deleteBtn.dataset.noticeDelete = "true";
        deleteBtn.setAttribute("role", "button");
        deleteBtn.setAttribute("aria-label", "隐藏通知");
        deleteBtn.textContent = "×";

        button.appendChild(meta);
        button.appendChild(content);
        button.appendChild(deleteBtn);
        return button;
    }

    function render() {
        const items = getVisibleItems();
        state.unreadCount = countUnread(items);
        list.innerHTML = "";

        items.forEach(function (item) {
            list.appendChild(createNoticeElement(item));
        });

        empty.textContent = state.emptyText;
        empty.hidden = items.length !== 0;
        badge.hidden = state.unreadCount === 0;
        readAllButton.disabled = state.unreadCount === 0;
    }

    function setItems(items, unreadCount) {
        state.items = Array.isArray(items) ? items.map(notificationCommon.normalizeItem) : [];
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function upsertItem(item, unreadCount) {
        const normalized = notificationCommon.normalizeItem(item);
        const index = state.items.findIndex(function (existing) {
            return String(existing.id) === String(normalized.id);
        });
        if (index >= 0) {
            state.items[index] = normalized;
        } else {
            state.items.unshift(normalized);
        }
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function applyReadAll(ids, unreadCount) {
        const idSet = new Set((ids || []).map(function (id) {
            return String(id);
        }));
        state.items = state.items.map(function (item) {
            if (idSet.size === 0 || idSet.has(String(item.id))) {
                return { ...item, read: true };
            }
            return item;
        });
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function removeItem(id, unreadCount) {
        state.items = state.items.filter(function (item) {
            return String(item.id) !== String(id);
        });
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function removeItems(ids, unreadCount) {
        const idSet = new Set((ids || []).map(function (id) {
            return String(id);
        }));
        state.items = idSet.size === 0
            ? []
            : state.items.filter(function (item) {
                return !idSet.has(String(item.id));
            });
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function setLoading(text) {
        state.emptyText = text || "通知加载中...";
        state.items = [];
        state.unreadCount = 0;
        render();
    }

    function closePanel() {
        panel.classList.remove("is-active");
        toggleButton.setAttribute("aria-expanded", "false");
    }

    function openPanel() {
        panel.classList.add("is-active");
        toggleButton.setAttribute("aria-expanded", "true");
    }

    toggleButton.addEventListener("click", function (event) {
        event.stopPropagation();
        if (panel.classList.contains("is-active")) {
            closePanel();
            return;
        }
        openPanel();
    });

    panel.addEventListener("click", function (event) {
        event.stopPropagation();
    });

    document.addEventListener("click", function (event) {
        if (!dropdown.contains(event.target)) {
            closePanel();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closePanel();
        }
    });

    list.addEventListener("click", function (event) {
        const deleteTarget = event.target.closest("[data-notice-delete]");
        const itemTarget = event.target.closest("[data-notice-item]");
        if (!itemTarget) {
            return;
        }

        const id = Number(itemTarget.dataset.noticeId);
        if (!Number.isFinite(id)) {
            return;
        }

        if (deleteTarget) {
            event.preventDefault();
            event.stopPropagation();
            window.dispatchEvent(new CustomEvent("notification:hide-popup", {
                detail: { id }
            }));
            return;
        }

        const item = state.items.find(function (entry) {
            return Number(entry.id) === id;
        });
        if (!item) {
            return;
        }

        if (!item.read) {
            window.dispatchEvent(new CustomEvent("notification:read", {
                detail: { id }
            }));
        }

        notificationCommon.openDetail(item, { title: "通知详情" });
        closePanel();
    });

    readAllButton.addEventListener("click", function () {
        if (state.unreadCount === 0) {
            return;
        }
        window.dispatchEvent(new CustomEvent("notification:read-all"));
    });

    window.addEventListener("notification:data-loading", function (event) {
        const detail = event.detail || {};
        setLoading(detail.text || "通知加载中...");
    });

    window.addEventListener("notification:data-sync", function (event) {
        const detail = event.detail || {};
        setItems(detail.items || [], detail.unreadCount || 0);
    });

    window.addEventListener("notification:data-created", function (event) {
        const detail = event.detail || {};
        if (detail.item) {
            upsertItem(detail.item, detail.unreadCount || 0);
        }
    });

    window.addEventListener("notification:data-updated", function (event) {
        const detail = event.detail || {};
        if (detail.item) {
            upsertItem(detail.item, detail.unreadCount || 0);
        }
    });

    window.addEventListener("notification:data-read-all", function (event) {
        const detail = event.detail || {};
        applyReadAll(detail.ids || [], detail.unreadCount || 0);
    });

    window.addEventListener("notification:data-deleted", function (event) {
        const detail = event.detail || {};
        if (detail.id) {
            removeItem(detail.id, detail.unreadCount || 0);
        }
    });

    window.addEventListener("notification:data-deleted-all", function (event) {
        const detail = event.detail || {};
        removeItems(detail.ids || [], detail.unreadCount || 0);
    });

    window.addEventListener("notification:action-error", function (event) {
        const detail = event.detail || {};
        if (detail.action === "HIDE_POPUP") {
            notificationCommon.showMessage(detail.message || "隐藏失败，请稍后重试", "error");
            return;
        }
        if (detail.action === "DELETE") {
            notificationCommon.showMessage(detail.message || "删除失败，请稍后重试", "error");
        }
    });

    window.notificationDropdownApi = {
        close: closePanel,
        setItems,
        upsertItem,
        applyReadAll,
        removeItem,
        removeItems,
        setLoading
    };

    setLoading("通知加载中...");
});
