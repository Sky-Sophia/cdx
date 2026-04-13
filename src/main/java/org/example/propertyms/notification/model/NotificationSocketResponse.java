package org.example.propertyms.notification.model;

import java.util.List;
import lombok.Data;

@Data
public class NotificationSocketResponse {
    private String type;
    private String message;
    private Integer unreadCount;
    private Integer dispatchCount;
    private List<NotificationItem> items;
    private NotificationItem item;
    private List<Long> ids;
    private Long id;

    public static NotificationSocketResponse sync(List<NotificationItem> items, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("sync");
        response.setItems(items);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse created(NotificationItem item, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("notice_created");
        response.setItem(item);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse updated(NotificationItem item, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("notice_updated");
        response.setItem(item);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse readAll(List<Long> ids, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("notice_read_all");
        response.setIds(ids);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse deleted(Long id, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("notice_deleted");
        response.setId(id);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse deletedAll(List<Long> ids, int unreadCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("notice_deleted_all");
        response.setIds(ids);
        response.setUnreadCount(unreadCount);
        return response;
    }

    public static NotificationSocketResponse sendAck(int dispatchCount) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("send_ack");
        response.setDispatchCount(dispatchCount);
        response.setMessage("发送成功");
        return response;
    }

    public static NotificationSocketResponse error(String message) {
        NotificationSocketResponse response = new NotificationSocketResponse();
        response.setType("error");
        response.setMessage(message);
        return response;
    }
}

