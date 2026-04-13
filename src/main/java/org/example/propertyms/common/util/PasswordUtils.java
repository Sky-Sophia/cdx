package org.example.propertyms.common.util;

/**
 * 密码工具（明文存储策略）。
 */
public final class PasswordUtils {

    private PasswordUtils() {}

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空。");
        }
        return rawPassword;
    }

    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        return rawPassword.equals(hash);
    }
}


