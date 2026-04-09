package org.example.propertyms.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
                       @RequestParam(defaultValue = "ACTIVE") String status,
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
        if (status == null || !VALID_STATUS.contains(status.toUpperCase())) {
            redirectAttributes.addFlashAttribute("error", "不支持的用户状态。");
            return "redirect:/admin/users/new";
        }

        try {
            User user = userService.register(username.trim(), password);
            userService.updateRole(user.getId(), role);
            userService.updateStatus(user.getId(), status.toUpperCase());
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

        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "用户列表_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("用户列表");

            // ── Header style ──
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // ── Body style ──
            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);

            // ── Header row ──
            String[] headers = {"用户名", "角色", "状态", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ──
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                Row row = sheet.createRow(i + 1);

                var c0 = row.createCell(0);
                c0.setCellValue(user.getUsername());
                c0.setCellStyle(bodyStyle);

                var c1 = row.createCell(1);
                c1.setCellValue(user.getRole() != null ? user.getRole().getLabel() : "");
                c1.setCellStyle(bodyStyle);

                var c2 = row.createCell(2);
                c2.setCellValue("ACTIVE".equals(user.getStatus()) ? "启用" : "禁用");
                c2.setCellStyle(bodyStyle);

                var c3 = row.createCell(3);
                c3.setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().format(dtFmt) : "");
                c3.setCellStyle(bodyStyle);
            }

            // ── Auto-size columns ──
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Add a little extra width for Chinese characters
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 4000));
            }

            wb.write(response.getOutputStream());
        }
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

