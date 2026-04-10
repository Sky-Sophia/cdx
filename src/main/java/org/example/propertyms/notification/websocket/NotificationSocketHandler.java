package org.example.propertyms.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
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
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationSessionRegistry notificationSessionRegistry;

    public NotificationSocketHandler(ObjectMapper objectMapper,
                                     NotificationService notificationService,
                                     NotificationSessionRegistry notificationSessionRegistry) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationSessionRegistry = notificationSessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UserSession currentUser = currentUser(session);
        if (currentUser == null || currentUser.getId() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            return;
        }
        notificationSessionRegistry.register(currentUser.getId(), session);
        syncInbox(session, currentUser.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserSession currentUser = currentUser(session);
        if (currentUser == null || currentUser.getId() == null) {
            send(session, NotificationSocketResponse.error("当前登录状态无效，请重新登录。"));
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            return;
        }

        try {
            NotificationSocketRequest request = objectMapper.readValue(message.getPayload(), NotificationSocketRequest.class);
            String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
            switch (action) {
                case "SYNC" -> syncInbox(session, currentUser.getId());
                case "SEND" -> handleSend(session, currentUser, request);
                case "READ" -> handleRead(currentUser.getId(), request);
                case "READ_ALL" -> handleReadAll(currentUser.getId());
                case "DELETE" -> handleDelete(currentUser.getId(), request);
                default -> send(session, NotificationSocketResponse.error("未识别的通知指令。"));
            }
        } catch (IllegalArgumentException ex) {
            send(session, NotificationSocketResponse.error(ex.getMessage()));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    private void handleSend(WebSocketSession session, UserSession currentUser, NotificationSocketRequest request) throws Exception {
        NotificationSendPayload payload = objectMapper.treeToValue(request.getPayload(), NotificationSendPayload.class);
        List<NotificationDispatchResult> dispatched = notificationService.send(currentUser, payload);
        for (NotificationDispatchResult dispatchResult : dispatched) {
            int unreadCount = notificationService.countUnread(dispatchResult.getReceiverId());
            pushToUser(dispatchResult.getReceiverId(),
                    NotificationSocketResponse.created(dispatchResult.getItem(), unreadCount));
        }
        send(session, NotificationSocketResponse.sendAck(dispatched.size()));
    }

    private void handleRead(Long userId, NotificationSocketRequest request) throws Exception {
        NotificationIdPayload payload = objectMapper.treeToValue(request.getPayload(), NotificationIdPayload.class);
        if (payload == null || payload.getId() == null) {
            throw new IllegalArgumentException("通知 ID 不能为空。");
        }
        NotificationItem item = notificationService.markRead(userId, payload.getId());
        int unreadCount = notificationService.countUnread(userId);
        pushToUser(userId, NotificationSocketResponse.updated(item, unreadCount));
    }

    private void handleReadAll(Long userId) throws Exception {
        List<Long> ids = notificationService.markAllRead(userId);
        int unreadCount = notificationService.countUnread(userId);
        pushToUser(userId, NotificationSocketResponse.readAll(ids, unreadCount));
    }

    private void handleDelete(Long userId, NotificationSocketRequest request) throws Exception {
        NotificationIdPayload payload = objectMapper.treeToValue(request.getPayload(), NotificationIdPayload.class);
        if (payload == null || payload.getId() == null) {
            throw new IllegalArgumentException("通知 ID 不能为空。");
        }
        Long id = notificationService.softDelete(userId, payload.getId());
        int unreadCount = notificationService.countUnread(userId);
        pushToUser(userId, NotificationSocketResponse.deleted(id, unreadCount));
    }

    private void syncInbox(WebSocketSession session, Long userId) throws Exception {
        List<NotificationItem> items = notificationService.loadInbox(userId, 30);
        int unreadCount = notificationService.countUnread(userId);
        send(session, NotificationSocketResponse.sync(items, unreadCount));
    }

    private void pushToUser(Long userId, NotificationSocketResponse response) throws Exception {
        for (WebSocketSession targetSession : notificationSessionRegistry.findSessions(userId)) {
            send(targetSession, response);
        }
    }

    private void send(WebSocketSession session, NotificationSocketResponse response) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
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
}
