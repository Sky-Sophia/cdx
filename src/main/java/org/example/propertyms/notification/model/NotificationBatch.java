package org.example.propertyms.notification.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationBatch {
    private Long id;
    private String batchNo;
    private String msgType;
    private String content;
    private Long senderId;
    private String targetType;
    private String targetValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
