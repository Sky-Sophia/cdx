package org.example.propertyms.notification.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationMessage {
    private Long id;
    private String batchNo;
    private String msgType;
    private String content;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime sendTime;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
    private String targetType;
    private String targetValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
