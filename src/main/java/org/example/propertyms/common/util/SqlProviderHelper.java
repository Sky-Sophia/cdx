package org.example.propertyms.common.util;

/**
 * SQL Provider 公用判空工具，消除各 SqlProvider 中重复的 isNotBlank 方法。
 */
public final class SqlProviderHelper {

    private SqlProviderHelper() {}

    public static boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}


