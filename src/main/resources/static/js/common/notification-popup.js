document.addEventListener("DOMContentLoaded", function () {
    const dropdown = document.querySelector("[data-notification-dropdown]");
    if (!dropdown) {
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
        const hh = String(date.getHours()).padStart(2, "0");
        const mm = String(date.getMinutes()).padStart(2, "0");

        if (dayDiff <= 0) {
            return `今天 ${hh}:${mm}`;
        }
        if (dayDiff === 1) {
            return `昨天 ${hh}:${mm}`;
        }
        if (dayDiff < 7) {
            return `${dayDiff}天前`;
        }
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    }

    function normalizeItem(item) {
        return {
            id: item.id,
            msgType: item.msgType || "",
            content: item.content || "",
            sender: item.sender || "",
            receiver: item.receiver || "",
            targetType: item.targetType || "",
            targetValue: item.targetValue || "",
            sendTime: item.sendTime || null,
            read: !!item.read,
            readTime: item.readTime || null
        };
    }

    function sortItems(items) {
        return items.slice().sort(function (left, right) {
            const unreadRankDiff = Number(left.read) - Number(right.read);
            if (unreadRankDiff !== 0) {
                return unreadRankDiff;
            }

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

    function countUnread(items) {
        return items.reduce(function (count, item) {
            return count + (isUnread(item) ? 1 : 0);
        }, 0);
    }

    function createNoticeElement(item) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `notice-item${isUnread(item) ? " unread" : ""}`;
        button.dataset.noticeItem = "true";
        button.dataset.noticeId = String(item.id);
        button.title = item.content || "";

        const time = document.createElement("span");
        time.className = "notice-time";
        time.textContent = formatTimeLabel(item.sendTime);

        const content = document.createElement("span");
        content.className = "notice-content";
        content.textContent = item.sender ? `${item.sender}：${item.content}` : item.content;

        const deleteBtn = document.createElement("span");
        deleteBtn.className = "notice-delete";
        deleteBtn.dataset.noticeDelete = "true";
        deleteBtn.setAttribute("role", "button");
        deleteBtn.setAttribute("aria-label", "删除通知");
        deleteBtn.textContent = "×";

        button.appendChild(time);
        button.appendChild(content);
        button.appendChild(deleteBtn);
        return button;
    }

    function render() {
        const items = sortItems(state.items);
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
        state.items = Array.isArray(items) ? items.map(normalizeItem) : [];
        if (typeof unreadCount === "number") {
            state.unreadCount = unreadCount;
        }
        render();
    }

    function upsertItem(item, unreadCount) {
        const normalized = normalizeItem(item);
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
            window.dispatchEvent(new CustomEvent("notification:delete", {
                detail: { id: id }
            }));
            return;
        }

        const item = state.items.find(function (entry) {
            return Number(entry.id) === id;
        });
        if (item && !item.read) {
            window.dispatchEvent(new CustomEvent("notification:read", {
                detail: { id: id }
            }));
        }
    });

    readAllButton.addEventListener("click", function () {
        if (state.unreadCount === 0) {
            return;
        }
        window.dispatchEvent(new CustomEvent("notification:read-all"));
    });

    window.notificationDropdownApi = {
        close: closePanel,
        setItems: setItems,
        upsertItem: upsertItem,
        applyReadAll: applyReadAll,
        removeItem: removeItem,
        setLoading: setLoading
    };

    setLoading("通知加载中...");
});
