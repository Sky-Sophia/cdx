package org.example.propertyms.user.model;

public enum Role {
    OFFICE,
    MANAGEMENT,
    ENGINEERING,
    USER;

    public String getLabel() {
        return switch (this) {
            case OFFICE -> "综合办公室（超级管理员）";
            case MANAGEMENT -> "管理部（普通管理员）";
            case ENGINEERING -> "工程部（维修师傅）";
            case USER -> "无部门（普通住户）";
        };
    }

    public boolean canAccessAdminConsole() {
        return this != USER;
    }

    public boolean canManageUsers() {
        return this != OFFICE;
    }
}
