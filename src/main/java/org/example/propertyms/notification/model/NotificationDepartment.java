package org.example.propertyms.notification.model;

import java.util.Locale;

import lombok.Getter;
import org.example.propertyms.user.model.Role;

@Getter
public enum NotificationDepartment {
    OFFICE("综合办公室", "OFFICE", 10, Role.SUPER_ADMIN),
    MANAGEMENT("管理部", "MANAGEMENT", 20, Role.ADMIN),
    FINANCE("财务部", "FINANCE", 30, Role.ACCOUNTANT),
    ENGINEERING("工程部", "ENGINEERING", 40, Role.ENGINEER);

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
            return null;
        }
        return switch (role) {
            case SUPER_ADMIN -> OFFICE;
            case ADMIN -> MANAGEMENT;
            case ACCOUNTANT -> FINANCE;
            case ENGINEER -> ENGINEERING;
            case RESIDENT -> null;
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
            case "administration", "office", "super_admin", "superadmin" -> OFFICE;
            case "admin", "management", "manager" -> MANAGEMENT;
            case "finance", "accountant" -> FINANCE;
            case "engineering", "engineer", "staff" -> ENGINEERING;
            default -> throw new IllegalArgumentException("未识别的部门类型：" + value);
        };
    }
}

