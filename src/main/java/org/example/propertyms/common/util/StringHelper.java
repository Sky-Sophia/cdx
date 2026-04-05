package org.example.propertyms.common.util;

/**
 * 通用字符串判空工具，消除各 ServiceImpl 中重复的 isBlank 方法。
 */
public final class StringHelper {

    private StringHelper() {}

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}

