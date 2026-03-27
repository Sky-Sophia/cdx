package org.example.javawebdemo.model;

public enum Role {
    ADMIN,
    STAFF,
    FINANCE,
    USER;

    public String getLabel() {
        return switch (this) {
            case ADMIN -> "系统管理员";
            case STAFF -> "物业管家";
            case FINANCE -> "财务专员";
            case USER -> "住户";
        };
    }
}
