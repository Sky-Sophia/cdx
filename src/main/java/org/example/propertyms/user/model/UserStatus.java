package org.example.propertyms.user.model;

import lombok.Getter;

/**
 * 用户状态枚举常量，替代硬编码字符串。
 */
@Getter
public enum UserStatus {
    ACTIVE("正常 · 已启用"),
    DISABLED("已停用");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

}


