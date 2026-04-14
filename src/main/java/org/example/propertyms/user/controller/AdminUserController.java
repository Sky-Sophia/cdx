package org.example.propertyms.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.common.util.ExcelExportHelper;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.common.web.ManagementPageRouter;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.DepartmentService;
import org.example.propertyms.user.service.UserDepartmentResolver;
import org.example.propertyms.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private static final Set<String> VALID_STATUS = Set.of("ACTIVE", "DISABLED");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;
    private final DepartmentService departmentService;
    private final UserDepartmentResolver userDepartmentResolver;

    public AdminUserController(UserService userService,
                               DepartmentService departmentService,
                               UserDepartmentResolver userDepartmentResolver) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.userDepartmentResolver = userDepartmentResolver;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) String status,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }

        return ManagementPageRouter.redirectToTab("users", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "userQ", q);
            if (role != null) {
                builder.queryParam("userRole", role.name());
            }
            ManagementPageRouter.addTrimmedParam(builder, "userStatus", status);
        });
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }

        redirectAttributes.addFlashAttribute("createUser", defaultCreateUser());
        redirectAttributes.addFlashAttribute("openCreateUserModal", true);
        return RedirectUrls.MANAGEMENT_USERS;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }

        User user = userService.findById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "用户不存在。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        if (user.getDepartmentCode() == null) {
            user.setDepartmentCode(userDepartmentResolver.defaultDepartmentCode(user.getRole()));
        }
        model.addAttribute("managedUser", user);
        model.addAttribute("editing", true);
        populateUserFormOptions(model);
        return "admin/users/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam Role role,
                       @RequestParam(required = false) String departmentCode,
                       @RequestParam(defaultValue = "ACTIVE") String status,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }
        if (isInvalidStatus(status)) {
            redirectAttributes.addFlashAttribute("error", "不支持的用户状态。");
            prepareCreateModalState(redirectAttributes, username, role, departmentCode, status);
            return RedirectUrls.MANAGEMENT_USERS;
        }

        try {
            User user = userService.register(username, password);
            userService.updateManagementProfile(user.getId(), role, departmentCode, status);
            redirectAttributes.addFlashAttribute("success", "用户已创建。");
            return RedirectUrls.MANAGEMENT_USERS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            prepareCreateModalState(redirectAttributes, username, role, departmentCode, status);
            return RedirectUrls.MANAGEMENT_USERS;
        }
    }

    @PostMapping("/{id}/manage")
    public String manage(@PathVariable Long id,
                         @RequestParam Role role,
                         @RequestParam(required = false) String departmentCode,
                         @RequestParam String status,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }
        if (isInvalidStatus(status)) {
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

        try {
            userService.updateManagementProfile(id, role, departmentCode, status);
            redirectAttributes.addFlashAttribute("success", "用户身份与状态已更新。");
            return RedirectUrls.MANAGEMENT_USERS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }
        if (isInvalidStatus(status)) {
            redirectAttributes.addFlashAttribute("error", "不支持的用户状态。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        UserSession currentUser = currentUser(session);
        if (currentUser != null && currentUser.getId().equals(id) && "DISABLED".equalsIgnoreCase(status)) {
            redirectAttributes.addFlashAttribute("error", "不能禁用当前登录账号。");
            return RedirectUrls.MANAGEMENT_USERS;
        }

        userService.updateStatus(id, status.toUpperCase(Locale.ROOT));
        redirectAttributes.addFlashAttribute("success", "用户状态已更新。");
        return RedirectUrls.MANAGEMENT_USERS;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam(required = false) String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (lacksAdminPermission(session)) {
            return denyUserManagementAccess(redirectAttributes);
        }

        String password = StringHelper.trimToNull(newPassword);
        if (password == null) {
            password = "Property@123";
        }
        try {
            userService.resetPassword(id, password);
            redirectAttributes.addFlashAttribute("success", "密码重置完成。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/edit/" + id;
    }

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Role role,
                            @RequestParam(required = false) String status,
                            HttpSession session,
                            HttpServletResponse response) throws IOException {
        if (lacksAdminPermission(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<User> users = userService.listByFilters(q, role, status);
        String[] headers = {"用户名", "身份类型", "状态", "创建时间"};
        ExcelExportHelper.export(response, "用户列表", "用户列表", headers, users, (row, user) -> {
            row.getCell(0).setCellValue(user.getUsername() != null ? user.getUsername() : "");
            row.getCell(1).setCellValue(user.getRole() != null ? user.getRole().getLabel() : "");
            row.getCell(2).setCellValue("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "启用" : "禁用");
            row.getCell(3).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().format(EXPORT_TIME_FORMATTER) : "");
        });
    }

    private String denyUserManagementAccess(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "仅超级管理员可访问用户管理。");
        return RedirectUrls.MANAGEMENT_DASHBOARD;
    }

    private boolean lacksAdminPermission(HttpSession session) {
        UserSession currentUser = currentUser(session);
        return currentUser == null
                || currentUser.getRole() == null
                || currentUser.getRole().canManageUserAccounts();
    }

    private UserSession currentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
    }

    private void populateUserFormOptions(Model model) {
        model.addAttribute("roles", Role.values());
        model.addAttribute("departmentOptions", departmentService.listEnabled());
    }

    private void prepareCreateModalState(RedirectAttributes redirectAttributes,
                                         String username,
                                         Role role,
                                         String departmentCode,
                                         String status) {
        User user = new User();
        user.setUsername(StringHelper.trimToNull(username));
        user.setRole(role == null ? Role.ADMIN : role);
        user.setDepartmentCode(defaultDepartmentCode(user.getRole(), departmentCode));
        user.setStatus(isInvalidStatus(status) ? "ACTIVE" : status.toUpperCase(Locale.ROOT));
        redirectAttributes.addFlashAttribute("createUser", user);
        redirectAttributes.addFlashAttribute("openCreateUserModal", true);
    }

    private boolean isInvalidStatus(String status) {
        return status == null || !VALID_STATUS.contains(status.toUpperCase(Locale.ROOT));
    }

    private String defaultDepartmentCode(Role role, String departmentCode) {
        String normalizedCode = StringHelper.upperCaseOrNull(departmentCode);
        if (normalizedCode != null) {
            return normalizedCode;
        }
        return userDepartmentResolver.defaultDepartmentCode(role);
    }

    private User defaultCreateUser() {
        User user = new User();
        user.setRole(Role.ADMIN);
        user.setDepartmentCode(userDepartmentResolver.defaultDepartmentCode(Role.ADMIN));
        user.setStatus("ACTIVE");
        return user;
    }
}
