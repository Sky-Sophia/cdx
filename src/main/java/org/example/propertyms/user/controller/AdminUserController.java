package org.example.propertyms.user.controller;

import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private static final Set<String> VALID_STATUS = Set.of("ACTIVE", "DISABLED");

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) String status,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "users");
        if (q != null && !q.isBlank()) {
            builder.queryParam("userQ", q);
        }
        if (role != null) {
            builder.queryParam("userRole", role.name());
        }
        if (status != null && !status.isBlank()) {
            builder.queryParam("userStatus", status);
        }
        return "redirect:" + builder.toUriString();
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }

        model.addAttribute("managedUser", new User());
        model.addAttribute("editing", false);
        model.addAttribute("roles", new Role[]{Role.ADMIN, Role.STAFF, Role.FINANCE});
        return "admin/users/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }

        User user = userService.findById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "用户不存在。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        model.addAttribute("managedUser", user);
        model.addAttribute("editing", true);
        model.addAttribute("roles", new Role[]{Role.ADMIN, Role.STAFF, Role.FINANCE});
        return "admin/users/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam Role role,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }
        if (role == Role.USER) {
            redirectAttributes.addFlashAttribute("error", "后台只允许创建管理员/员工/财务账号。");
            return "redirect:/admin/users/new";
        }

        try {
            User user = userService.register(username.trim(), password);
            userService.updateRole(user.getId(), role);
            userService.updateStatus(user.getId(), "ACTIVE");
            redirectAttributes.addFlashAttribute("success", "用户已创建。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/new";
        }
        return RedirectUrls.MANAGEMENT_USERS;
    }

    @PostMapping("/{id}/manage")
    public String manage(@PathVariable Long id,
                         @RequestParam Role role,
                         @RequestParam String status,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }
        if (role == Role.USER) {
            redirectAttributes.addFlashAttribute("error", "后台账号仅支持管理员、员工或财务角色。");
            return "redirect:/admin/users/edit/" + id;
        }
        if (status == null || !VALID_STATUS.contains(status.toUpperCase())) {
            redirectAttributes.addFlashAttribute("error", "不支持的用户状态。");
            return "redirect:/admin/users/edit/" + id;
        }

        User user = userService.findById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "用户不存在。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        UserSession currentUser = currentUser(session);
        if (currentUser != null && currentUser.getId().equals(id) && "DISABLED".equalsIgnoreCase(status)) {
            redirectAttributes.addFlashAttribute("error", "不能禁用当前登录账号。");
            return "redirect:/admin/users/edit/" + id;
        }

        userService.updateRole(id, role);
        userService.updateStatus(id, status.toUpperCase());
        redirectAttributes.addFlashAttribute("success", "用户权限与状态已更新。");
        return RedirectUrls.MANAGEMENT_USERS;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }
        if (status == null || !VALID_STATUS.contains(status.toUpperCase())) {
            redirectAttributes.addFlashAttribute("error", "不支持的用户状态。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        UserSession currentUser = currentUser(session);
        if (currentUser != null && currentUser.getId().equals(id) && "DISABLED".equalsIgnoreCase(status)) {
            redirectAttributes.addFlashAttribute("error", "不能禁用当前登录账号。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        userService.updateStatus(id, status.toUpperCase());
        redirectAttributes.addFlashAttribute("success", "用户状态已更新。");
        return RedirectUrls.MANAGEMENT_USERS;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam(required = false) String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            redirectAttributes.addFlashAttribute("error", "仅管理员可访问用户管理。");
            return RedirectUrls.MANAGEMENT_DASHBOARD;
        }

        String password = (newPassword == null || newPassword.isBlank()) ? "Property@123" : newPassword;
        try {
            userService.resetPassword(id, password);
            redirectAttributes.addFlashAttribute("success", "密码重置完成。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/edit/" + id;
    }

    private boolean lacksAdminPermission(HttpSession session) {
        UserSession currentUser = currentUser(session);
        return currentUser == null || currentUser.getRole() != Role.ADMIN;
    }

    private UserSession currentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
    }
}

