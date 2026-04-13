package org.example.propertyms.account.controller;

import jakarta.servlet.http.HttpSession;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.example.propertyms.notification.service.NotificationService;
import org.example.propertyms.user.model.Role;
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
    private final NotificationService notificationService;

    public AccountController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
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
        model.addAttribute("departmentLabel", departmentLabel(user));
        model.addAttribute("statusLabel", statusLabel(user.getStatus()));
        model.addAttribute("profileInboxItems", notificationService.loadInbox(currentUser.getId(), 100));
        model.addAttribute("profileInboxUnreadCount", notificationService.countUnread(currentUser.getId()));
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

    private String departmentLabel(User user) {
        if (user == null || user.getRole() == Role.RESIDENT) {
            return "普通住户";
        }

        String departmentCode = user.getDepartmentCode();
        if (departmentCode != null && !departmentCode.isBlank()) {
            try {
                return NotificationDepartment.from(departmentCode).getLabel();
            } catch (IllegalArgumentException ignored) {
                return departmentCode.trim();
            }
        }

        NotificationDepartment defaultDepartment = NotificationDepartment.defaultForRole(user.getRole());
        return defaultDepartment != null ? defaultDepartment.getLabel() : "普通住户";
    }
}


