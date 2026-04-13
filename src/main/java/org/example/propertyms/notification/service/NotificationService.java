package org.example.propertyms.notification.service;

import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.notification.model.NotificationDispatchResult;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.model.NotificationSendPayload;

public interface NotificationService {
    List<NotificationItem> loadInbox(Long receiverId, int limit);

    int countUnread(Long receiverId);

    List<NotificationDispatchResult> send(UserSession sender, NotificationSendPayload payload);

    NotificationItem markRead(Long receiverId, Long notificationId);

    List<Long> markAllRead(Long receiverId);

    Long delete(Long receiverId, Long notificationId);

    List<Long> deleteAll(Long receiverId);
}

