package org.example.propertyms.notification.model;

import lombok.Data;

@Data
public class NotificationSendPayload {
    private String msgType;
    private String content;
    private String targetType;
    private String receiver;
}
