package org.example.propertyms.notification.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class NotificationSocketRequest {
    private String action;
    private JsonNode payload;
}
