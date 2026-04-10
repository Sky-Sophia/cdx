package org.example.propertyms.notification.model;

import java.util.Locale;
import org.example.propertyms.user.model.Role;

public enum NotificationDepartment {
    ADMINISTRATION("行政部", "ADMINISTRATION", Role.ADMIN),
    FINANCE("财务部", "FINANCE", Role.FINANCE),
    ENGINEERING("工程部", "ENGINEERING", Role.STAFF),
    SECURITY("安保部", "SECURITY", Role.STAFF);

    private final String label;
    private final String code;
    private final Role fallbackRole;

    NotificationDepartment(String label, String code, Role fallbackRole) {
        this.label = label;
        this.code = code;
        this.fallbackRole = fallbackRole;
    }

    public String getCode() {
        return code;
    }

    public Role getFallbackRole() {
        return fallbackRole;
    }

    public static NotificationDepartment from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("部门名称不能为空。");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (NotificationDepartment department : values()) {
            if (department.label.equals(value.trim())
                    || department.code.equalsIgnoreCase(value.trim())
                    || department.fallbackRole.name().equalsIgnoreCase(normalized)) {
                return department;
            }
        }
        throw new IllegalArgumentException("未识别的部门类型：" + value);
    }
}
