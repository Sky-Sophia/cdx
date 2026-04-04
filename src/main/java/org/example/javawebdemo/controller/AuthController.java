package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.javawebdemo.dto.LoginRequest;
import org.example.javawebdemo.dto.RegisterRequest;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(10);
    private static final Map<String, LoginAttempt> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(defaultValue = "login") String tab,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SessionKeys.CURRENT_USER) != null) {
            return "redirect:/admin/management?tab=dashboard";
        }
        model.addAttribute("tab", "register".equalsIgnoreCase(tab) ? "register" : "login");
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Validated LoginRequest request,
                        BindingResult bindingResult,
                        HttpServletRequest httpServletRequest,
                        HttpSession session,
                        Model model) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        String attemptKey = buildAttemptKey(httpServletRequest, username);
        model.addAttribute("tab", "login");
        model.addAttribute("loginUsername", username);

        if (isLoginBlocked(attemptKey, model)) {
            return "auth/login";
        }

        if (bindingResult.hasErrors() || username.isEmpty() || password.isBlank()) {
            model.addAttribute("error", "璇疯緭鍏ユ纭殑鐢ㄦ埛鍚嶅拰瀵嗙爜銆?");
            registerFailedAttempt(attemptKey);
            return "auth/login";
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            model.addAttribute("error", "璐﹀彿鎴栧瘑鐮侀敊璇紝鎴栬处鍙峰凡琚鐢ㄣ€?");
            registerFailedAttempt(attemptKey);
            return "auth/login";
        }

        clearLoginAttempts(attemptKey);
        session.invalidate();
        HttpSession newSession = httpServletRequest.getSession(true);
        newSession.setAttribute(SessionKeys.CURRENT_USER, new UserSession(user.getId(), user.getUsername(), user.getRole()));
        return "redirect:/admin/management?tab=dashboard";
    }

    @PostMapping("/register")
    public String register(@Validated RegisterRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        model.addAttribute("tab", "register");
        model.addAttribute("registerUsername", username);

        if (bindingResult.hasErrors() || username.isEmpty()) {
            model.addAttribute("error", "璇疯緭鍏ユ纭殑娉ㄥ唽淇℃伅銆?");
            return "auth/login";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "涓ゆ杈撳叆鐨勫瘑鐮佷笉涓€鑷淬€?");
            return "auth/login";
        }

        try {
            User user = userService.register(username, request.getPassword());
            userService.updateRole(user.getId(), Role.STAFF);
            userService.updateStatus(user.getId(), "DISABLED");
            redirectAttributes.addFlashAttribute("success", "娉ㄥ唽鎴愬姛锛屽緟绠＄悊鍛樺鏍稿惎鐢ㄥ悗鍙櫥褰曘€?");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerRedirect() {
        return "redirect:/login?tab=register";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            return "redirect:/login";
        }
        return "profile";
    }

    @GetMapping("/profile/password")
    public String passwordRedirect() {
        return "redirect:/admin/management?tab=dashboard";
    }

    private String buildAttemptKey(HttpServletRequest request, String username) {
        String remoteIp = request == null ? "unknown" : request.getRemoteAddr();
        return remoteIp + "|" + username.toLowerCase(Locale.ROOT);
    }

    private boolean isLoginBlocked(String key, Model model) {
        LoginAttempt attempt = LOGIN_ATTEMPTS.get(key);
        if (attempt == null || attempt.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lockedUntil())) {
            LOGIN_ATTEMPTS.remove(key);
            return false;
        }
        long seconds = Duration.between(Instant.now(), attempt.lockedUntil()).toSeconds();
        long remainSeconds = Math.max(seconds, 1);
        model.addAttribute("error", "鐧诲綍澶辫触娆℃暟杩囧锛岃 " + remainSeconds + " 绉掑悗鍐嶈瘯銆?");
        return true;
    }

    private void registerFailedAttempt(String key) {
        LOGIN_ATTEMPTS.compute(key, (k, oldValue) -> {
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

    private void clearLoginAttempts(String key) {
        LOGIN_ATTEMPTS.remove(key);
    }

    private record LoginAttempt(int failedCount, Instant lockedUntil) {
    }
}
