package org.example.propertyms.notification.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {
    private final NotificationSocketHandler notificationSocketHandler;
    private final NotificationHandshakeInterceptor notificationHandshakeInterceptor;

    public NotificationWebSocketConfig(NotificationSocketHandler notificationSocketHandler,
                                       NotificationHandshakeInterceptor notificationHandshakeInterceptor) {
        this.notificationSocketHandler = notificationSocketHandler;
        this.notificationHandshakeInterceptor = notificationHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationSocketHandler, "/ws/notifications")
                .addInterceptors(notificationHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
