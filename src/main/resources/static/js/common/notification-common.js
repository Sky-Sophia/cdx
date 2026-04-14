(() => {
    "use strict";

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

    function pad(number) {
        return String(number).padStart(2, "0");
    }

    function formatRelativeTime(value, options) {
        const config = {
            fallbackText: "刚刚",
            todayPrefix: "今天 ",
            yesterdayPrefix: "昨天 ",
            recentDaySuffix: "天前",
            maxRelativeDays: 6,
            includeTimeForSameDay: true,
            includeTimeForYesterday: true,
            includeTimeInAbsolute: false,
            ...options
        };

        const date = parseDate(value);
        if (!date) {
            return config.fallbackText;
        }

        const now = new Date();
        const msPerDay = 24 * 60 * 60 * 1000;
        const dayDiff = Math.floor((startOfDay(now).getTime() - startOfDay(date).getTime()) / msPerDay);
        const timeText = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
        const dateText = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

        if (dayDiff <= 0) {
            return config.includeTimeForSameDay ? `${config.todayPrefix}${timeText}` : timeText;
        }
        if (dayDiff === 1) {
            return config.includeTimeForYesterday ? `${config.yesterdayPrefix}${timeText}` : config.yesterdayPrefix.trim();
        }
        if (dayDiff <= config.maxRelativeDays) {
            return `${dayDiff}${config.recentDaySuffix}`;
        }
        return config.includeTimeInAbsolute ? `${dateText} ${timeText}` : dateText;
    }

    function resolveTypeMeta(msgType) {
        const label = String(msgType || "").trim() || "通知";
        const lowerType = label.toLowerCase();

        if (label === "公告" || lowerType.includes("announce")) {
            return {
                label,
                tone: "announcement",
                historyType: "announce",
                iconPath: "/icons/speaker.svg"
            };
        }
        if (label === "提醒" || lowerType.includes("remind")) {
            return {
                label,
                tone: "reminder",
                historyType: "reminder",
                iconPath: "/icons/device.svg"
            };
        }
        if (label === "预警" || lowerType.includes("warn") || lowerType.includes("alert")) {
            return {
                label,
                tone: "warning",
                historyType: "warning",
                iconPath: "/icons/shield.svg"
            };
        }
        if (lowerType.includes("bill")) {
            return {
                label,
                tone: "bill",
                historyType: "notice",
                iconPath: "/icons/bell.svg"
            };
        }
        if (lowerType.includes("work")) {
            return {
                label,
                tone: "work",
                historyType: "notice",
                iconPath: "/icons/bell.svg"
            };
        }
        return {
            label,
            tone: "notice",
            historyType: "notice",
            iconPath: "/icons/bell.svg"
        };
    }

    function normalizeItem(item) {
        return {
            id: item?.id,
            msgType: item?.msgType || "",
            content: item?.content || "",
            sender: item?.sender || "",
            receiver: item?.receiver || "",
            targetType: item?.targetType || "",
            targetValue: item?.targetValue || "",
            sendTime: item?.sendTime || null,
            read: !!item?.read,
            readTime: item?.readTime || null,
            popupHidden: !!item?.popupHidden,
            popupHiddenTime: item?.popupHiddenTime || null
        };
    }

    function openDetail(item, options) {
        if (!item) {
            return;
        }
        window.dispatchEvent(new CustomEvent("notification:detail-open", {
            detail: {
                item: { ...item },
                options: options || {}
            }
        }));
    }

    function showMessage(message, type) {
        if (window.ValidationBubble && typeof window.ValidationBubble.showMessage === "function") {
            window.ValidationBubble.showMessage(message, type || "error");
            return;
        }
        window.console[type === "success" ? "info" : "warn"](message);
    }

    window.NotificationCommon = {
        formatRelativeTime,
        normalizeItem,
        openDetail,
        parseDate,
        resolveTypeMeta,
        showMessage,
        startOfDay
    };
})();
