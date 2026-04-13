package org.example.propertyms.notification.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationItem {
    private Long id;
    private String msgType;
    private String content;
    private String sender;
    private String receiver;
    private String targetType;
    private String targetValue;
    private LocalDateTime sendTime;
    private boolean read;
    private LocalDateTime readTime;
}

