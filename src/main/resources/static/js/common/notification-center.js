document.addEventListener("DOMContentLoaded", function () {
    const notificationCommon = window.NotificationCommon;
    const dropdown = document.querySelector("[data-notification-dropdown]");
    const hasHistoryModal = Boolean(document.querySelector("[data-history-modal]"));
    if ((!dropdown && !hasHistoryModal) || !notificationCommon) {
        return;
    }

    const wsPath = dropdown ? (dropdown.getAttribute("data-ws-url") || "/ws/notifications") : "/ws/notifications";
    let socket = null;
    let reconnectTimer = 0;
    let pendingSend = null;
    let lastAction = "";

    const socketActionHandlers = {
        SYNC(detail) {
            emit("notification:data-sync", {
                items: detail.items || [],
                unreadCount: detail.unreadCount || 0
            });
        },
        READ(detail) {
            emit("notification:data-updated", {
                item: detail.item,
                unreadCount: detail.unreadCount || 0
            });
        },
        HIDE_POPUP(detail) {
            emit("notification:data-updated", {
                item: detail.item,
                unreadCount: detail.unreadCount || 0
            });
        },
        READ_ALL(detail) {
            emit("notification:data-read-all", {
                ids: detail.ids || [],
                unreadCount: detail.unreadCount || 0
            });
        },
        DELETE(detail) {
            emit("notification:data-deleted", {
                id: detail.id,
                unreadCount: detail.unreadCount || 0
            });
        },
        DELETE_ALL(detail) {
            emit("notification:data-deleted-all", {
                ids: detail.ids || [],
                unreadCount: detail.unreadCount || 0
            });
        }
    };

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

    function showActionError(action, message) {
        emit("notification:action-error", {
            action,
            message
        });
    }

    function connect() {
        clearReconnectTimer();
        emit("notification:data-loading", { text: "通知加载中..." });

        socket = new WebSocket(buildWsUrl(wsPath));

        socket.addEventListener("open", function () {
            sendSocketAction("SYNC");
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

        if (data.type === "send_ack") {
            if (pendingSend) {
                pendingSend.onSuccess(data);
                pendingSend = null;
            }
            lastAction = "";
            return;
        }

        if (data.type === "error") {
            const message = data.message || "通知操作失败";
            if (pendingSend) {
                pendingSend.onError(message);
                pendingSend = null;
            } else {
                showActionError(lastAction || "", message);
                window.console.warn(message);
            }
            lastAction = "";
            return;
        }

        const eventMap = {
            sync: { action: "SYNC", handler: socketActionHandlers.SYNC },
            notice_created: {
                action: "",
                handler(detail) {
                    emit("notification:data-created", {
                        item: detail.item,
                        unreadCount: detail.unreadCount || 0
                    });
                }
            },
            notice_updated: { action: lastAction, handler: socketActionHandlers[lastAction] || socketActionHandlers.READ },
            notice_read_all: { action: "READ_ALL", handler: socketActionHandlers.READ_ALL },
            notice_deleted: { action: "DELETE", handler: socketActionHandlers.DELETE },
            notice_deleted_all: { action: "DELETE_ALL", handler: socketActionHandlers.DELETE_ALL }
        };

        const mapping = eventMap[data.type];
        if (!mapping || typeof mapping.handler !== "function") {
            return;
        }

        mapping.handler(data);
        if (!mapping.action || lastAction === mapping.action) {
            lastAction = "";
        }
    }

    function canUseSocket() {
        return socket && socket.readyState === WebSocket.OPEN;
    }

    function sendSocketAction(action, payload) {
        if (!canUseSocket()) {
            showActionError(action, "通知通道未连接，请稍后重试。");
            return false;
        }
        lastAction = action;
        socket.send(JSON.stringify({
            action,
            payload: payload || null
        }));
        return true;
    }

    function requestJson(url, method, action, successHandler, fallbackMessage) {
        lastAction = action;
        return window.fetch(url, {
            method,
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
                showActionError(action, fallbackMessage);
                window.console.error(`通知请求失败: ${action}`, error);
                return false;
            });
    }

    function requestDelete(url, action, successHandler) {
        return requestJson(url, "DELETE", action, successHandler, "删除失败，请稍后重试");
    }

    function requestPatch(url, action, successHandler) {
        return requestJson(url, "PATCH", action, successHandler, "通知操作失败，请稍后重试");
    }

    function bindSocketAction(eventName, action, payloadBuilder, fallbackHandler) {
        window.addEventListener(eventName, function (event) {
            const detail = event.detail || {};
            const payload = typeof payloadBuilder === "function" ? payloadBuilder(detail) : null;
            if (payloadBuilder && !payload) {
                return;
            }
            if (sendSocketAction(action, payload)) {
                return;
            }
            if (typeof fallbackHandler === "function") {
                fallbackHandler(detail);
            }
        });
    }

    function createApi() {
        return {
            isReady: canUseSocket,
            read(id) {
                return sendSocketAction("READ", { id });
            },
            readViaSocket(id) {
                return this.read(id);
            },
            readAll() {
                return sendSocketAction("READ_ALL");
            },
            readAllViaSocket() {
                return this.readAll();
            },
            hidePopup(id) {
                if (sendSocketAction("HIDE_POPUP", { id })) {
                    return Promise.resolve(true);
                }
                return requestPatch(`/api/notifications/${id}/popup-hide`, "HIDE_POPUP", function (result) {
                    socketActionHandlers.HIDE_POPUP({
                        item: result.item || null,
                        unreadCount: Number(result.unreadCount || 0)
                    });
                });
            },
            hidePopupViaSocket(id) {
                return sendSocketAction("HIDE_POPUP", { id });
            },
            delete(id) {
                return requestDelete(`/api/notifications/${id}`, "DELETE", function (result) {
                    socketActionHandlers.DELETE({
                        id: result.id || id,
                        unreadCount: Number(result.unreadCount || 0)
                    });
                });
            },
            deleteViaSocket(id) {
                return sendSocketAction("DELETE", { id });
            },
            deleteAll() {
                return requestDelete("/api/notifications", "DELETE_ALL", function (result) {
                    socketActionHandlers.DELETE_ALL({
                        ids: result.ids || [],
                        unreadCount: Number(result.unreadCount || 0)
                    });
                });
            },
            deleteAllViaSocket() {
                return sendSocketAction("DELETE_ALL");
            },
            sync() {
                return sendSocketAction("SYNC");
            }
        };
    }

    window.addEventListener("compose-notice:submit", function (event) {
        const detail = event.detail || {};
        detail.handled = true;

        if (!canUseSocket()) {
            detail.onError && detail.onError("通知通道未连接，请稍后重试。");
            return;
        }

        pendingSend = detail;
        const sent = sendSocketAction("SEND", detail.payload || {});
        if (!sent) {
            pendingSend = null;
            detail.onError && detail.onError("通知通道未连接，请稍后重试。");
        }
    });

    bindSocketAction("notification:read", "READ", function (detail) {
        return detail.id ? { id: detail.id } : null;
    });

    bindSocketAction("notification:read-all", "READ_ALL");

    bindSocketAction("notification:hide-popup", "HIDE_POPUP", function (detail) {
        return detail.id ? { id: detail.id } : null;
    }, function (detail) {
        requestPatch(`/api/notifications/${detail.id}/popup-hide`, "HIDE_POPUP", function (result) {
            socketActionHandlers.HIDE_POPUP({
                item: result.item || null,
                unreadCount: Number(result.unreadCount || 0)
            });
        });
    });

    window.addEventListener("notification:delete", function (event) {
        const detail = event.detail || {};
        if (detail.id) {
            window.notificationCenterApi.delete(detail.id);
        }
    });

    window.addEventListener("notification:delete-all", function () {
        window.notificationCenterApi.deleteAll();
    });

    window.notificationCenterApi = createApi();
    connect();
});
