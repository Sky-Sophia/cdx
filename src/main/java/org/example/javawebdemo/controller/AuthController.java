package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpSession;
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
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(defaultValue = "login") String tab,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SessionKeys.CURRENT_USER) != null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("tab", "register".equalsIgnoreCase(tab) ? "register" : "login");
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Validated LoginRequest request,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        model.addAttribute("tab", "login");
        model.addAttribute("loginUsername", username);

        if (bindingResult.hasErrors() || username.isEmpty() || password.isBlank()) {
            model.addAttribute("error", "请输入正确的用户名和密码。");
            return "auth/login";
        }

        User user = userService.authenticate(username, password);
        if (user == null) {
            model.addAttribute("error", "账号或密码错误，或账户已被禁用。");
            return "auth/login";
        }

        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(user.getId(), user.getUsername(), user.getRole()));
        return "redirect:/admin/dashboard";
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
            model.addAttribute("error", "请输入正确的注册信息。");
            return "auth/login";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "两次输入的密码不一致。");
            return "auth/login";
        }

        try {
            User user = userService.register(username, request.getPassword());
            userService.updateRole(user.getId(), Role.STAFF);
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录系统。");
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
    public String profileRedirect() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/profile/password")
    public String passwordRedirect() {
        return "redirect:/admin/dashboard";
    }
}
