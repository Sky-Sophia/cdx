package org.example.javawebdemo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtils {
    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private PasswordUtils() {
    }

    public static String generateSalt() {
        // New passwords use bcrypt and do not require an external salt value.
        return "";
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空。");
        }
        return BCRYPT_PREFIX + BCRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String salt, String hash) {
        if (rawPassword == null || salt == null || hash == null) {
            return false;
        }
        if (hash.startsWith(BCRYPT_PREFIX)) {
            return BCRYPT.matches(rawPassword, hash.substring(BCRYPT_PREFIX.length()));
        }
        // Backward-compatible verification for existing SHA-256 + salt accounts.
        return legacySha256Hash(rawPassword, salt).equals(hash);
    }

    public static boolean isLegacyHash(String hash) {
        return hash != null && !hash.startsWith(BCRYPT_PREFIX);
    }

    private static String legacySha256Hash(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = salt + rawPassword;
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash password", ex);
        }
    }
}
