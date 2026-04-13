package org.example.propertyms.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 登录速率限制器，从 AuthController 中提取以遵循单一职责。
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(10);
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public String buildAttemptKey(HttpServletRequest request, String username) {
        String remoteIp = request == null ? "unknown" : request.getRemoteAddr();
        return remoteIp + "|" + username.toLowerCase(Locale.ROOT);
    }

    public boolean isBlocked(String key, Model model) {
        LoginAttempt attempt = loginAttempts.get(key);
        if (attempt == null || attempt.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lockedUntil())) {
            loginAttempts.remove(key);
            return false;
        }
        long seconds = Duration.between(Instant.now(), attempt.lockedUntil()).toSeconds();
        long remainSeconds = Math.max(seconds, 1);
        model.addAttribute("error", "登录失败次数过多，请 " + remainSeconds + " 秒后重试");
        return true;
    }

    public void registerFailure(String key) {
        loginAttempts.compute(key, (k, oldValue) -> {
            if (oldValue == null || (oldValue.lockedUntil() != null && Instant.now().isAfter(oldValue.lockedUntil()))) {
                return new LoginAttempt(1, null);
            }
            int failed = oldValue.failedCount() + 1;
            if (failed >= MAX_FAILED_ATTEMPTS) {
                return new LoginAttempt(failed, Instant.now().plus(LOGIN_LOCK_DURATION));
            }
            return new LoginAttempt(failed, oldValue.lockedUntil());
        });
    }

    public void clear(String key) {
        loginAttempts.remove(key);
    }

    private record LoginAttempt(int failedCount, Instant lockedUntil) {}
}


