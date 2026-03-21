package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.example.javawebdemo.dto.ChangePasswordRequest;
import org.example.javawebdemo.dto.LoginRequest;
import org.example.javawebdemo.dto.RegisterRequest;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderStatus;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.OrderService;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;
    private final OrderService orderService;
    private final MessageSource messageSource;

    public AuthController(UserService userService, OrderService orderService, MessageSource messageSource) {
        this.userService = userService;
        this.orderService = orderService;
        this.messageSource = messageSource;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        UserSession current = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (current != null) {
            return redirectByRole(current);
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Validated LoginRequest request,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", msg("auth.login.required"));
            model.addAttribute("username", normalize(request.getUsername()));
            return "auth/login";
        }
        String normalized = normalize(request.getUsername());
        User user = userService.authenticate(normalized, request.getPassword());
        if (user == null) {
            model.addAttribute("error", msg("auth.login.invalid"));
            model.addAttribute("username", normalized);
            return "auth/login";
        }
        UserSession userSession = new UserSession(user.getId(), user.getUsername(), user.getRole());
        session.setAttribute(SessionKeys.CURRENT_USER, userSession);
        return redirectByRole(userSession);
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        UserSession current = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (current != null) {
            return redirectByRole(current);
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Validated RegisterRequest request,
                           BindingResult bindingResult,
                           Model model) {
        String normalized = normalize(request.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", firstError(bindingResult));
            model.addAttribute("username", normalized);
            return "auth/register";
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", msg("auth.password.mismatch"));
            model.addAttribute("username", normalized);
            return "auth/register";
        }
        try {
            userService.register(normalized, request.getPassword());
            model.addAttribute("success", msg("auth.register.success"));
            model.addAttribute("username", normalized);
            return "auth/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("username", normalized);
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        UserSession userSession = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (userSession == null) {
            return "redirect:/login";
        }
        User user = userService.findById(userSession.getId());
        List<Order> orders = orderService.listByUser(userSession.getId());
        List<Order> recentOrders = orders.size() > 5 ? orders.subList(0, 5) : orders;
        long pendingCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .count();
        long paidCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();
        long refundedCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.REFUNDED)
                .count();
        model.addAttribute("user", user);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("orderTotal", orders.size());
        model.addAttribute("orderPending", pendingCount);
        model.addAttribute("orderPaid", paidCount);
        model.addAttribute("orderRefunded", refundedCount);
        return "profile/index";
    }

    @GetMapping("/profile/password")
    public String passwordPage(HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", msg("auth.login.first"));
            return "redirect:/login";
        }
        model.addAttribute("user", userService.findById(user.getId()));
        return "auth/password";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Validated ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", msg("auth.login.first"));
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return "redirect:/profile/password";
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", msg("auth.password.mismatch"));
            return "redirect:/profile/password";
        }
        try {
            userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("success", msg("auth.password.change.success"));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile/password";
        }
        return "redirect:/profile";
    }

    private String redirectByRole(UserSession user) {
        if (user.getRole().name().equals("ADMIN")) {
            return "redirect:/admin/shows";
        }
        if (user.getRole().name().equals("STAFF")) {
            return "redirect:/staff";
        }
        return "redirect:/";
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String firstError(BindingResult bindingResult) {
        if (bindingResult == null || bindingResult.getAllErrors().isEmpty()) {
            return msg("auth.validation.failed");
        }
        return bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    private String msg(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
