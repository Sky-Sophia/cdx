package org.example.propertyms.notification.model;

import java.util.Locale;

public enum NotificationTargetType {
    SINGLE,
    ALL,
    BUILDING,
    DEPARTMENT,
    DUE_BILL,
    WORK_ORDER_DONE;

    public static NotificationTargetType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("通知目标类型不能为空。");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "single" -> SINGLE;
            case "all" -> ALL;
            case "building" -> BUILDING;
            case "department" -> DEPARTMENT;
            case "due_bill", "unpaid" -> DUE_BILL;
            case "work_order_done", "work-order-done" -> WORK_ORDER_DONE;
            default -> throw new IllegalArgumentException("暂不支持的通知目标类型：" + value);
        };
    }
}
