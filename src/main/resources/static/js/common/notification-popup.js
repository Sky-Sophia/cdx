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

    function showMessage(message, type) {
        if (window.ValidationBubble && typeof window.ValidationBubble.showMessage === "function") {
            window.ValidationBubble.showMessage(message, type || "error");
            return;
        }
        window.console[type === "success" ? "info" : "warn"](message);
    }

    function deleteNotice(id) {
        return window.fetch(`/api/notifications/${id}`, {
            method: "DELETE",
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            },
            credentials: "same-origin"
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                return response.json();
            })
            .then(function (result) {
                window.dispatchEvent(new CustomEvent("notification:data-deleted", {
                    detail: {
                        id: result.id || id,
                        unreadCount: Number(result.unreadCount || 0)
                    }
                }));
            })
            .catch(function (error) {
                window.console.error("删除通知失败", error);
                if (window.notificationCenterApi
                    && typeof window.notificationCenterApi.deleteViaSocket === "function"
                    && window.notificationCenterApi.deleteViaSocket(id)) {
                    return;
                }
                showMessage("删除失败，请稍后重试", "error");
            });
    }

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

    function resolveTypeLabel(msgType) {
        const type = String(msgType || "").trim();
        return type || "\u901a\u77e5";
    }

    function resolveTypeTone(msgType) {
        const type = resolveTypeLabel(msgType);
        const lowerType = type.toLowerCase();

        switch (type) {
            case "\u516c\u544a":
                return "announcement";
            case "\u901a\u77e5":
                return "notice";
            case "\u63d0\u9192":
                return "reminder";
            case "\u9884\u8b66":
                return "warning";
            default:
                break;
        }

        if (lowerType.includes("announce")) {
            return "announcement";
        }
        if (lowerType.includes("warn") || lowerType.includes("alert")) {
            return "warning";
        }
        if (lowerType.includes("remind")) {
            return "reminder";
        }
        if (lowerType.includes("bill")) {
            return "bill";
        }
        if (lowerType.includes("work")) {
            return "work";
        }
        return "notice";
    }

    function createNoticeElement(item) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `notice-item${isUnread(item) ? " unread" : ""}`;
        button.dataset.noticeItem = "true";
        button.dataset.noticeId = String(item.id);
        button.dataset.noticeType = resolveTypeLabel(item.msgType);
        button.title = item.content || "";

        const meta = document.createElement("span");
        meta.className = "notice-meta";

        const typeTag = document.createElement("span");
        typeTag.className = "notice-type";
        typeTag.dataset.tone = resolveTypeTone(item.msgType);
        typeTag.textContent = resolveTypeLabel(item.msgType);

        const time = document.createElement("span");
        time.className = "notice-time";
        time.textContent = formatTimeLabel(item.sendTime);
        meta.appendChild(typeTag);
        meta.appendChild(time);

        const content = document.createElement("span");
        content.className = "notice-content";
        content.textContent = item.sender ? `${item.sender}：${item.content}` : item.content;

        const deleteBtn = document.createElement("span");
        deleteBtn.className = "notice-delete";
        deleteBtn.dataset.noticeDelete = "true";
        deleteBtn.setAttribute("role", "button");
        deleteBtn.setAttribute("aria-label", "删除通知");
        deleteBtn.textContent = "×";

        button.appendChild(meta);
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

    function removeItems(ids, unreadCount) {
        const idSet = new Set((ids || []).map(function (id) {
            return String(id);
        }));
        if (idSet.size === 0) {
            state.items = [];
        } else {
            state.items = state.items.filter(function (item) {
                return !idSet.has(String(item.id));
            });
        }
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
            deleteNotice(id);
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
        if (detail.action === "DELETE") {
            showMessage(detail.message || "删除失败，请稍后重试", "error");
        }
    });

    window.notificationDropdownApi = {
        close: closePanel,
        setItems: setItems,
        upsertItem: upsertItem,
        applyReadAll: applyReadAll,
        removeItem: removeItem,
        removeItems: removeItems,
        setLoading: setLoading
    };

    setLoading("通知加载中...");
});
