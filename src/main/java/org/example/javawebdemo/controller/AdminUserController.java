package org.example.javawebdemo.controller;

import java.util.List;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.UserService;
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
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) String status,
                       Model model) {
        List<User> users = userService.listByFilters(q, role, status);
        long total = users.size();
        long active = users.stream()
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .count();
        long disabled = total - active;
        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("totalUsers", total);
        model.addAttribute("activeUsers", active);
        model.addAttribute("disabledUsers", disabled);
        return "admin/users";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam Role role,
                         @RequestParam String status,
                         RedirectAttributes redirectAttributes) {
        userService.updateRole(id, role);
        userService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "用户信息已更新");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam(required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        String finalPassword = newPassword;
        if (finalPassword == null || finalPassword.isBlank()) {
            finalPassword = "123456";
        }
        if (finalPassword.length() < 6 || finalPassword.length() > 32) {
            redirectAttributes.addFlashAttribute("error", "密码长度需要在 6-32 之间");
            return "redirect:/admin/users";
        }
        userService.resetPassword(id, finalPassword);
        redirectAttributes.addFlashAttribute("success", "密码已重置");
        return "redirect:/admin/users";
    }
}
