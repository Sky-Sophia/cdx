package org.example.javawebdemo.model;

public enum Role {
    ADMIN,
    STAFF,
    USER;

    public String getLabel() {
        return switch (this) {
            case ADMIN -> "管理员";
            case STAFF -> "员工";
            case USER -> "用户";
        };
    }
}
