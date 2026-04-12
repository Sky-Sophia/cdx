document.addEventListener("DOMContentLoaded", function () {
    const dropdown = document.querySelector("[data-notification-dropdown]");
    const hasHistoryModal = Boolean(document.querySelector("[data-history-modal]"));
    if (!dropdown && !hasHistoryModal) {
        return;
    }

    const wsPath = dropdown ? (dropdown.getAttribute("data-ws-url") || "/ws/notifications") : "/ws/notifications";
    let socket = null;
    let reconnectTimer = 0;
    let pendingSend = null;
    let lastAction = "";

    function emit(name, detail) {
        window.dispatchEvent(new CustomEvent(name, {
            detail: detail || {}
        }));
    }

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
        emit("notification:data-loading", { text: "通知加载中..." });

        socket = new WebSocket(buildWsUrl(wsPath));

        socket.addEventListener("open", function () {
            lastAction = "SYNC";
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
                if (lastAction === "SYNC") {
                    lastAction = "";
                }
                emit("notification:data-sync", {
                    items: data.items || [],
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "notice_created":
                emit("notification:data-created", {
                    item: data.item,
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "notice_updated":
                if (lastAction === "READ") {
                    lastAction = "";
                }
                emit("notification:data-updated", {
                    item: data.item,
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "notice_read_all":
                if (lastAction === "READ_ALL") {
                    lastAction = "";
                }
                emit("notification:data-read-all", {
                    ids: data.ids || [],
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "notice_deleted":
                if (lastAction === "DELETE") {
                    lastAction = "";
                }
                emit("notification:data-deleted", {
                    id: data.id,
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "notice_deleted_all":
                if (lastAction === "DELETE_ALL") {
                    lastAction = "";
                }
                emit("notification:data-deleted-all", {
                    ids: data.ids || [],
                    unreadCount: data.unreadCount || 0
                });
                break;
            case "send_ack":
                if (pendingSend) {
                    pendingSend.onSuccess(data);
                    pendingSend = null;
                }
                lastAction = "";
                break;
            case "error":
                if (pendingSend) {
                    pendingSend.onError(data.message || "通知发送失败");
                    pendingSend = null;
                } else {
                    emit("notification:action-error", {
                        action: lastAction || "",
                        message: data.message || "通知操作失败"
                    });
                    window.console.warn(data.message || "通知操作失败");
                }
                lastAction = "";
                break;
            default:
                break;
        }
    }

    function canUseSocket() {
        return socket && socket.readyState === WebSocket.OPEN;
    }

    function requestDelete(url, action, successHandler) {
        lastAction = action;
        return window.fetch(url, {
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
                lastAction = "";
                successHandler(result || {});
                return true;
            })
            .catch(function (error) {
                lastAction = "";
                emit("notification:action-error", {
                    action: action,
                    message: "删除失败，请稍后重试"
                });
                window.console.error("通知删除失败", error);
                return false;
            });
    }

    function sendAction(action, payload) {
        if (!canUseSocket()) {
            emit("notification:action-error", {
                action: action,
                message: "通知通道未连接，请稍后重试"
            });
            return false;
        }
        lastAction = action;
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
            requestDelete(`/api/notifications/${detail.id}`, "DELETE", function (result) {
                emit("notification:data-deleted", {
                    id: result.id || detail.id,
                    unreadCount: Number(result.unreadCount || 0)
                });
            });
        }
    });

    window.addEventListener("notification:delete-all", function () {
        requestDelete("/api/notifications", "DELETE_ALL", function (result) {
            emit("notification:data-deleted-all", {
                ids: result.ids || [],
                unreadCount: Number(result.unreadCount || 0)
            });
        });
    });

    window.notificationCenterApi = {
        isReady: canUseSocket,
        readViaSocket: function (id) {
            return sendAction("READ", { id: id });
        },
        read: function (id) {
            return sendAction("READ", { id: id });
        },
        readAllViaSocket: function () {
            return sendAction("READ_ALL");
        },
        readAll: function () {
            return sendAction("READ_ALL");
        },
        deleteViaSocket: function (id) {
            return sendAction("DELETE", { id: id });
        },
        delete: function (id) {
            return requestDelete(`/api/notifications/${id}`, "DELETE", function (result) {
                emit("notification:data-deleted", {
                    id: result.id || id,
                    unreadCount: Number(result.unreadCount || 0)
                });
            });
        },
        deleteAllViaSocket: function () {
            return sendAction("DELETE_ALL");
        },
        deleteAll: function () {
            return requestDelete("/api/notifications", "DELETE_ALL", function (result) {
                emit("notification:data-deleted-all", {
                    ids: result.ids || [],
                    unreadCount: Number(result.unreadCount || 0)
                });
            });
        },
        sync: function () {
            return sendAction("SYNC");
        }
    };

    connect();
});
