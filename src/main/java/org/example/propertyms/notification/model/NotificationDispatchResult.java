package org.example.propertyms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDispatchResult {
    private Long receiverId;
    private NotificationItem item;
}
