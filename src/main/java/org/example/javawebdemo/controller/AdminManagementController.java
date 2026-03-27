package org.example.javawebdemo.controller;

import org.example.javawebdemo.mapper.BuildingMapper;
import org.example.javawebdemo.mapper.FeeBillMapper;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.mapper.ResidentMapper;
import org.example.javawebdemo.mapper.WorkOrderMapper;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.service.PropertyDashboardService;
import org.example.javawebdemo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminManagementController {
    private final PropertyUnitMapper propertyUnitMapper;
    private final BuildingMapper buildingMapper;
    private final ResidentMapper residentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final FeeBillMapper feeBillMapper;
    private final PropertyDashboardService propertyDashboardService;
    private final UserService userService;

    public AdminManagementController(PropertyUnitMapper propertyUnitMapper,
                                     BuildingMapper buildingMapper,
                                     ResidentMapper residentMapper,
                                     WorkOrderMapper workOrderMapper,
                                     FeeBillMapper feeBillMapper,
                                     PropertyDashboardService propertyDashboardService,
                                     UserService userService) {
        this.propertyUnitMapper = propertyUnitMapper;
        this.buildingMapper = buildingMapper;
        this.residentMapper = residentMapper;
        this.workOrderMapper = workOrderMapper;
        this.feeBillMapper = feeBillMapper;
        this.propertyDashboardService = propertyDashboardService;
        this.userService = userService;
    }

    @GetMapping("/management")
    public String management(@RequestParam(defaultValue = "dashboard") String tab,
                             @RequestParam(required = false) String unitKeyword,
                             @RequestParam(required = false) Long unitBuildingId,
                             @RequestParam(required = false) String unitStatus,
                             @RequestParam(required = false) String residentKeyword,
                             @RequestParam(required = false) String residentStatus,
                             @RequestParam(required = false) String workOrderStatus,
                             @RequestParam(required = false) String workOrderPriority,
                             @RequestParam(required = false) String billStatus,
                             @RequestParam(required = false) String billBillingMonth,
                             @RequestParam(required = false) String userQ,
                             @RequestParam(required = false) Role userRole,
                             @RequestParam(required = false) String userStatus,
                             Model model) {
        String currentTab = normalizeTab(tab);
        model.addAttribute("currentTab", currentTab);
        model.addAttribute("responseTip", "已切换到统一管理页面，减少跨页面跳转。");

        model.addAttribute("stats", propertyDashboardService.stats());
        model.addAttribute("recentOrders", propertyDashboardService.recentOrders(6));
        model.addAttribute("dueBills", propertyDashboardService.dueBills(6));

        model.addAttribute("units", propertyUnitMapper.findAll(unitKeyword, unitBuildingId, unitStatus));
        model.addAttribute("buildings", buildingMapper.findAll());
        model.addAttribute("unitKeyword", unitKeyword);
        model.addAttribute("unitBuildingId", unitBuildingId);
        model.addAttribute("unitStatus", unitStatus);

        model.addAttribute("residents", residentMapper.findAll(residentKeyword, residentStatus));
        model.addAttribute("residentKeyword", residentKeyword);
        model.addAttribute("residentStatus", residentStatus);

        model.addAttribute("orders", workOrderMapper.findAll(workOrderStatus, workOrderPriority));
        model.addAttribute("workOrderStatus", workOrderStatus);
        model.addAttribute("workOrderPriority", workOrderPriority);

        model.addAttribute("bills", feeBillMapper.findAll(billStatus, billBillingMonth));
        model.addAttribute("billStatus", billStatus);
        model.addAttribute("billBillingMonth", billBillingMonth);

        model.addAttribute("users", userService.listByFilters(userQ, userRole, userStatus));
        model.addAttribute("roles", Role.values());
        model.addAttribute("userQ", userQ);
        model.addAttribute("userRole", userRole);
        model.addAttribute("userStatus", userStatus);

        return "admin/management";
    }

    private String normalizeTab(String tab) {
        if ("dashboard".equals(tab)
                || "units".equals(tab)
                || "residents".equals(tab)
                || "work-orders".equals(tab)
                || "bills".equals(tab)
                || "users".equals(tab)) {
            return tab;
        }
        return "dashboard";
    }
}
