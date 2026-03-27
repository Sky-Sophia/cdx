package org.example.javawebdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/orders")
public class StaffOrderController {

    @GetMapping
    public String list() {
        return "redirect:/admin/work-orders";
    }
}
