package org.example.javawebdemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @GetMapping
    public String list() {
        return "redirect:/admin/work-orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id) {
        return "redirect:/admin/work-orders";
    }

    @PostMapping("/{showId}/create")
    public String create(@PathVariable Long showId) {
        return "redirect:/admin/work-orders";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id) {
        return "redirect:/admin/work-orders";
    }

    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id) {
        return "redirect:/admin/work-orders";
    }
}
