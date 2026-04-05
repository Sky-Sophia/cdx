package org.example.propertyms.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.propertyms.auth.dto.LoginRequest;
import org.example.propertyms.auth.dto.RegisterRequest;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.auth.service.LoginRateLimiter;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
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

    private final UserService userService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(UserService userService, LoginRateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(defaultValue = "login") String tab,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SessionKeys.CURRENT_USER) != null) {
            return RedirectUrls.MANAGEMENT_DASHBOARD;
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
        String attemptKey = rateLimiter.buildAttemptKey(httpServletRequest, username);

        model.addAttribute("tab", "login");
        model.addAttribute("loginUsername", username);

        if (rateLimiter.isBlocked(attemptKey, model)) {
            return "auth/login";
        }
        if (bindingResult.hasErrors() || username.isEmpty() || password.isBlank()) {
            model.addAttribute("error", "请输入正确的用户名和密码");
            rateLimiter.registerFailure(attemptKey);
            return "auth/login";
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            model.addAttribute("error", "账号或密码错误，或账号已被禁用");
            rateLimiter.registerFailure(attemptKey);
            return "auth/login";
        }

        rateLimiter.clear(attemptKey);
        session.invalidate();
        HttpSession newSession = httpServletRequest.getSession(true);
        newSession.setAttribute(SessionKeys.CURRENT_USER,
                new UserSession(user.getId(), user.getUsername(), user.getRole()));
        return RedirectUrls.MANAGEMENT_DASHBOARD;
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
            model.addAttribute("error", "请输入正确的注册信息");
            return "auth/login";
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "两次输入的密码不一致");
            return "auth/login";
        }
        try {
            User user = userService.register(username, request.getPassword());
            userService.updateRole(user.getId(), Role.STAFF);
            userService.updateStatus(user.getId(), "DISABLED");
            redirectAttributes.addFlashAttribute("success", "注册成功，待管理员审核启用后可登录");
            return RedirectUrls.LOGIN;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return RedirectUrls.LOGIN;
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        model.addAttribute("tab", "login");
        if (username == null || username.isBlank()) {
            model.addAttribute("error", "请输入用户名。");
            return "auth/login";
        }
        if (newPassword == null || newPassword.isBlank()) {
            model.addAttribute("error", "请输入新密码。");
            return "auth/login";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致。");
            return "auth/login";
        }
        User user = userService.findByUsername(username.trim());
        if (user == null) {
            model.addAttribute("error", "用户不存在，请检查用户名。");
            return "auth/login";
        }
        try {
            userService.resetPassword(user.getId(), newPassword);
            redirectAttributes.addFlashAttribute("success", "密码重置成功，请使用新密码登录。");
            return RedirectUrls.LOGIN;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerRedirect() {
        return "redirect:/login?tab=register";
    }
}

