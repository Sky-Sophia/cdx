package org.example.javawebdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping
    public String root() {
        return "redirect:/admin/management?tab=dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/admin/management?tab=dashboard";
    }
}
