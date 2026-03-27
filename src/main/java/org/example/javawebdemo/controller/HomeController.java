package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpSession;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            return "redirect:/login";
        }
        return "redirect:/admin/dashboard";
    }
}
