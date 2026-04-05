package org.example.propertyms.account.controller;

import jakarta.servlet.http.HttpSession;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 个人中心控制器，从 AuthController 中拆分出来。
 */
@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            return RedirectUrls.LOGIN;
        }

        User user = userService.findById(currentUser.getId());
        if (user == null) {
            session.invalidate();
            return RedirectUrls.LOGIN;
        }

        model.addAttribute("userProfile", user);
        model.addAttribute("roleLabel", user.getRole() != null ? user.getRole().getLabel() : "未设置角色");
        model.addAttribute("statusLabel", statusLabel(user.getStatus()));
        return "account/profile";
    }

    @GetMapping("/profile/password")
    public String passwordRedirect() {
        return RedirectUrls.MANAGEMENT_DASHBOARD;
    }

    private String statusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "状态未知";
        }
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "正常 · 已启用";
            case "DISABLED" -> "已停用";
            default -> status;
        };
    }
}

