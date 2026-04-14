package org.example.propertyms.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.notification.model.NotificationDispatchResult;
import org.example.propertyms.notification.model.NotificationIdPayload;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.model.NotificationSendPayload;
import org.example.propertyms.notification.model.NotificationSocketRequest;
import org.example.propertyms.notification.model.NotificationSocketResponse;
import org.example.propertyms.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationSocketHandler extends TextWebSocketHandler {
    private static final String ACTION_SYNC = "SYNC";
    private static final String ACTION_SEND = "SEND";
    private static final String ACTION_READ = "READ";
    private static final String ACTION_READ_ALL = "READ_ALL";
    private static final String ACTION_HIDE_POPUP = "HIDE_POPUP";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_DELETE_ALL = "DELETE_ALL";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationSessionRegistry notificationSessionRegistry;
    private final ConcurrentHashMap<String, Object> sessionLockMap = new ConcurrentHashMap<>();

    public NotificationSocketHandler(ObjectMapper objectMapper,
                                     NotificationService notificationService,
                                     NotificationSessionRegistry notificationSessionRegistry) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationSessionRegistry = notificationSessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        UserSession currentUser = currentUser(session);
        if (currentUser == null || currentUser.getId() == null) {
            closeUnauthorized(session);
            return;
        }
        notificationSessionRegistry.register(currentUser.getId(), session);
        syncInbox(session, currentUser.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        UserSession currentUser = currentUser(session);
        if (currentUser == null || currentUser.getId() == null) {
            send(session, NotificationSocketResponse.error("当前登录状态无效，请重新登录。"));
            closeUnauthorized(session);
            return;
        }

        try {
            NotificationSocketRequest request = objectMapper.readValue(message.getPayload(), NotificationSocketRequest.class);
            dispatchAction(session, currentUser, request);
        } catch (IllegalArgumentException ex) {
            send(session, NotificationSocketResponse.error(ex.getMessage()));
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionLockMap.remove(session.getId());
        unregister(session);
    }

    private void dispatchAction(WebSocketSession session,
                                UserSession currentUser,
                                NotificationSocketRequest request) throws Exception {
        String action = StringHelper.upperCaseOrNull(request == null ? null : request.getAction());
        if (action == null) {
            throw new IllegalArgumentException("未识别的通知指令。");
        }

        switch (action) {
            case ACTION_SYNC -> syncInbox(session, currentUser.getId());
            case ACTION_SEND -> handleSend(session, currentUser, request);
            case ACTION_READ -> pushUpdatedItem(currentUser.getId(),
                    notificationService.markRead(currentUser.getId(), requireNotificationId(request)));
            case ACTION_READ_ALL -> pushToUser(currentUser.getId(),
                    NotificationSocketResponse.readAll(
                            notificationService.markAllRead(currentUser.getId()),
                            unreadCount(currentUser.getId())));
            case ACTION_HIDE_POPUP -> pushUpdatedItem(currentUser.getId(),
                    notificationService.hidePopup(currentUser.getId(), requireNotificationId(request)));
            case ACTION_DELETE -> pushToUser(currentUser.getId(),
                    NotificationSocketResponse.deleted(
                            notificationService.delete(currentUser.getId(), requireNotificationId(request)),
                            unreadCount(currentUser.getId())));
            case ACTION_DELETE_ALL -> pushToUser(currentUser.getId(),
                    NotificationSocketResponse.deletedAll(
                            notificationService.deleteAll(currentUser.getId()),
                            unreadCount(currentUser.getId())));
            default -> send(session, NotificationSocketResponse.error("未识别的通知指令。"));
        }
    }

    private void handleSend(WebSocketSession session,
                            UserSession currentUser,
                            NotificationSocketRequest request) throws Exception {
        NotificationSendPayload payload = readPayload(request, NotificationSendPayload.class);
        List<NotificationDispatchResult> dispatched = notificationService.send(currentUser, payload);
        for (NotificationDispatchResult dispatchResult : dispatched) {
            pushToUser(dispatchResult.getReceiverId(),
                    NotificationSocketResponse.created(dispatchResult.getItem(), unreadCount(dispatchResult.getReceiverId())));
        }
        send(session, NotificationSocketResponse.sendAck(dispatched.size()));
    }

    private void syncInbox(WebSocketSession session, Long userId) throws Exception {
        List<NotificationItem> items = notificationService.loadInbox(userId, 100);
        send(session, NotificationSocketResponse.sync(items, unreadCount(userId)));
    }

    private void pushUpdatedItem(Long userId, NotificationItem item) throws Exception {
        pushToUser(userId, NotificationSocketResponse.updated(item, unreadCount(userId)));
    }

    private void pushToUser(Long userId, NotificationSocketResponse response) throws Exception {
        for (WebSocketSession targetSession : notificationSessionRegistry.findSessions(userId)) {
            send(targetSession, response);
        }
    }

    private int unreadCount(Long userId) {
        return notificationService.countUnread(userId);
    }

    private Long requireNotificationId(NotificationSocketRequest request) throws IOException {
        NotificationIdPayload payload = readPayload(request, NotificationIdPayload.class);
        if (payload == null || payload.getId() == null) {
            throw new IllegalArgumentException("通知 ID 不能为空。");
        }
        return payload.getId();
    }

    private <T> T readPayload(NotificationSocketRequest request, Class<T> type) throws IOException {
        if (request == null || request.getPayload() == null || request.getPayload().isNull()) {
            return null;
        }
        return objectMapper.treeToValue(request.getPayload(), type);
    }

    private void send(WebSocketSession session, NotificationSocketResponse response) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        Object lock = sessionLockMap.computeIfAbsent(session.getId(), key -> new Object());
        synchronized (lock) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    private UserSession currentUser(WebSocketSession session) {
        Object value = session.getAttributes().get(NotificationHandshakeInterceptor.CURRENT_USER_ATTR);
        return value instanceof UserSession userSession ? userSession : null;
    }

    private void unregister(WebSocketSession session) {
        UserSession currentUser = currentUser(session);
        if (currentUser != null) {
            notificationSessionRegistry.unregister(currentUser.getId(), session);
        }
    }

    private void closeUnauthorized(WebSocketSession session) throws IOException {
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
    }
}
