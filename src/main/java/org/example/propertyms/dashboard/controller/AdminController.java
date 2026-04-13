package org.example.propertyms.dashboard.controller;

import org.example.propertyms.common.constant.RedirectUrls;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping
    public String root() {
        return RedirectUrls.MANAGEMENT_DASHBOARD;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return RedirectUrls.MANAGEMENT_DASHBOARD;
    }
}


