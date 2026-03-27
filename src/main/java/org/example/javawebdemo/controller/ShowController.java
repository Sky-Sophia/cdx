package org.example.javawebdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shows")
public class ShowController {

    @GetMapping
    public String list() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        return "redirect:/admin/dashboard";
    }
}
