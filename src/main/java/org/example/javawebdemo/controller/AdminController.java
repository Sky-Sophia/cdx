package org.example.javawebdemo.controller;

import org.example.javawebdemo.model.DashboardStats;
import org.example.javawebdemo.service.PropertyDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final PropertyDashboardService propertyDashboardService;

    public AdminController(PropertyDashboardService propertyDashboardService) {
        this.propertyDashboardService = propertyDashboardService;
    }

    @GetMapping
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStats stats = propertyDashboardService.stats();
        model.addAttribute("stats", stats);
        model.addAttribute("recentOrders", propertyDashboardService.recentOrders(6));
        model.addAttribute("dueBills", propertyDashboardService.dueBills(6));
        return "admin/dashboard";
    }
}
