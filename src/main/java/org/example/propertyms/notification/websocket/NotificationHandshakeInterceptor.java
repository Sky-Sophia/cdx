package org.example.propertyms.notification.websocket;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

import lombok.NonNull;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {
    public static final String CURRENT_USER_ATTR = "notificationCurrentUser";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        HttpSession session = servletRequest.getServletRequest().getSession(false);
        UserSession currentUser = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(CURRENT_USER_ATTR, currentUser);
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}

