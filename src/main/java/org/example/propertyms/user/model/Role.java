package org.example.propertyms.user.model;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    ENGINEER,
    ACCOUNTANT,
    RESIDENT;

    public String getLabel() {
        return switch (this) {
            case SUPER_ADMIN -> "超级管理员";
            case ADMIN -> "普通管理员";
            case ENGINEER -> "维修工程师";
            case ACCOUNTANT -> "财务会计";
            case RESIDENT -> "普通住户";
        };
    }

    public boolean canAccessAdminConsole() {
        return this != RESIDENT;
    }

    public boolean canManageUserAccounts() {
        return this == SUPER_ADMIN;
    }
}
