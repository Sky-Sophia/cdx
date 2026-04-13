package org.example.propertyms.common.web;

import jakarta.servlet.http.HttpSession;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.service.DepartmentService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAdvice {
    private final BuildingService buildingService;
    private final DepartmentService departmentService;

    public GlobalViewModelAdvice(BuildingService buildingService, DepartmentService departmentService) {
        this.buildingService = buildingService;
        this.departmentService = departmentService;
    }

    @ModelAttribute
    public void populateComposeOptions(HttpSession session, Model model) {
        if (session == null || session.getAttribute(SessionKeys.CURRENT_USER) == null) {
            return;
        }
        model.addAttribute("notificationBuildings", buildingService.listAll());
        model.addAttribute("notificationDepartments", departmentService.listEnabled());
    }
}

