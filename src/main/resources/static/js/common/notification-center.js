document.addEventListener("DOMContentLoaded", function () {
    const dropdown = document.querySelector("[data-notification-dropdown]");
    const api = window.notificationDropdownApi;
    if (!dropdown || !api) {
        return;
    }

    const wsPath = dropdown.getAttribute("data-ws-url") || "/ws/notifications";
    let socket = null;
    let reconnectTimer = 0;
    let pendingSend = null;

    function buildWsUrl(path) {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        return `${protocol}//${window.location.host}${path}`;
    }

    function clearReconnectTimer() {
        if (reconnectTimer) {
            window.clearTimeout(reconnectTimer);
            reconnectTimer = 0;
        }
    }

    function connect() {
        clearReconnectTimer();
        api.setLoading("通知加载中...");

        socket = new WebSocket(buildWsUrl(wsPath));

        socket.addEventListener("open", function () {
            socket.send(JSON.stringify({ action: "SYNC" }));
        });

        socket.addEventListener("message", function (event) {
            handleMessage(event.data);
        });

        socket.addEventListener("close", function () {
            if (pendingSend) {
                pendingSend.onError("通知连接已断开，请稍后重试。");
                pendingSend = null;
            }
            scheduleReconnect();
        });

        socket.addEventListener("error", function () {
            if (socket) {
                socket.close();
            }
        });
    }

    function scheduleReconnect() {
        if (reconnectTimer) {
            return;
        }
        reconnectTimer = window.setTimeout(function () {
            reconnectTimer = 0;
            connect();
        }, 3000);
    }

    function handleMessage(raw) {
        let data;
        try {
            data = JSON.parse(raw);
        } catch (error) {
            window.console.error("通知消息解析失败", error);
            return;
        }

        switch (data.type) {
            case "sync":
                api.setItems(data.items || [], data.unreadCount || 0);
                break;
            case "notice_created":
                api.upsertItem(data.item, data.unreadCount || 0);
                break;
            case "notice_updated":
                api.upsertItem(data.item, data.unreadCount || 0);
                break;
            case "notice_read_all":
                api.applyReadAll(data.ids || [], data.unreadCount || 0);
                break;
            case "notice_deleted":
                api.removeItem(data.id, data.unreadCount || 0);
                break;
            case "send_ack":
                if (pendingSend) {
                    pendingSend.onSuccess(data);
                    pendingSend = null;
                }
                break;
            case "error":
                if (pendingSend) {
                    pendingSend.onError(data.message || "通知发送失败");
                    pendingSend = null;
                } else {
                    window.console.warn(data.message || "通知操作失败");
                }
                break;
            default:
                break;
        }
    }

    function canUseSocket() {
        return socket && socket.readyState === WebSocket.OPEN;
    }

    function sendAction(action, payload) {
        if (!canUseSocket()) {
            return false;
        }
        socket.send(JSON.stringify({
            action: action,
            payload: payload || null
        }));
        return true;
    }

    window.addEventListener("compose-notice:submit", function (event) {
        const detail = event.detail || {};
        detail.handled = true;

        if (!canUseSocket()) {
            detail.onError && detail.onError("通知通道未连接，请稍后重试。");
            return;
        }

        pendingSend = detail;
        const sent = sendAction("SEND", detail.payload || {});
        if (!sent) {
            pendingSend = null;
            detail.onError && detail.onError("通知通道未连接，请稍后重试。");
        }
    });

    window.addEventListener("notification:read", function (event) {
        const detail = event.detail || {};
        if (detail.id) {
            sendAction("READ", { id: detail.id });
        }
    });

    window.addEventListener("notification:read-all", function () {
        sendAction("READ_ALL");
    });

    window.addEventListener("notification:delete", function (event) {
        const detail = event.detail || {};
        if (detail.id) {
            sendAction("DELETE", { id: detail.id });
        }
    });

    connect();
});
