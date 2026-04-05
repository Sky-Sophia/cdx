package org.example.propertyms.dashboard.controller;

import jakarta.servlet.http.HttpSession;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            return RedirectUrls.LOGIN;
        }
        return RedirectUrls.MANAGEMENT_DASHBOARD;
    }
}

