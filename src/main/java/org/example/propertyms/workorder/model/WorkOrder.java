package org.example.propertyms.workorder.model;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class WorkOrder {
    private Long id;
    private String orderNo;
    private Long unitId;
    private String unitNo;
    private String residentName;
    private String phone;
    private String category;
    private String priority;
    private String description;
    private String status;
    private String assignee;
    private LocalDateTime createdAt;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;
    private LocalDateTime finishedAt;
}

