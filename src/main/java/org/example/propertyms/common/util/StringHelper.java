package org.example.propertyms.common.util;

import java.util.Locale;

/**
 * 通用字符串处理工具，集中收敛空值、裁剪与大小写归一化逻辑。
 */
public final class StringHelper {

    private StringHelper() {}

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    public static String upperCaseOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    public static String lowerCaseOrNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
