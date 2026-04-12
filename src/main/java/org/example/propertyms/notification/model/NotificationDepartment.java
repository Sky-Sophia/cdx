package org.example.propertyms.notification.model;

import java.util.Locale;

import lombok.Getter;
import org.example.propertyms.user.model.Role;

@Getter
public enum NotificationDepartment {
    OFFICE("综合办公室", "OFFICE", 10, Role.OFFICE),
    MANAGEMENT("管理部", "MANAGEMENT", 20, Role.MANAGEMENT),
    ENGINEERING("工程部", "ENGINEERING", 30, Role.ENGINEERING),
    NONE("无部门", "NONE", 40, Role.USER);

    private final String label;
    private final String code;
    private final int sortOrder;
    private final Role fallbackRole;

    NotificationDepartment(String label, String code, int sortOrder, Role fallbackRole) {
        this.label = label;
        this.code = code;
        this.sortOrder = sortOrder;
        this.fallbackRole = fallbackRole;
    }

    public static NotificationDepartment defaultForRole(Role role) {
        if (role == null) {
            return NONE;
        }
        return switch (role) {
            case OFFICE -> OFFICE;
            case MANAGEMENT -> MANAGEMENT;
            case ENGINEERING -> ENGINEERING;
            case USER -> NONE;
        };
    }

    public static NotificationDepartment from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("部门名称不能为空。");
        }
        String trimmed = value.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        for (NotificationDepartment department : values()) {
            if (department.label.equals(trimmed)
                    || department.code.equalsIgnoreCase(trimmed)
                    || department.fallbackRole.name().equalsIgnoreCase(normalized)) {
                return department;
            }
        }

        return switch (normalized) {
            case "administration", "admin" -> OFFICE;
            case "finance", "security" -> MANAGEMENT;
            case "staff" -> ENGINEERING;
            default -> throw new IllegalArgumentException("未识别的部门类型：" + value);
        };
    }
}
