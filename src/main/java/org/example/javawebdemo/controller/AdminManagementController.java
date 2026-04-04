package org.example.javawebdemo.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.service.BuildingService;
import org.example.javawebdemo.service.FeeBillService;
import org.example.javawebdemo.service.PropertyDashboardService;
import org.example.javawebdemo.service.PropertyUnitService;
import org.example.javawebdemo.service.ResidentService;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminManagementController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PropertyUnitService propertyUnitService;
    private final BuildingService buildingService;
    private final ResidentService residentService;
    private final WorkOrderService workOrderService;
    private final FeeBillService feeBillService;
    private final PropertyDashboardService propertyDashboardService;
    private final UserService userService;

    public AdminManagementController(PropertyUnitService propertyUnitService,
                                     BuildingService buildingService,
                                     ResidentService residentService,
                                     WorkOrderService workOrderService,
                                     FeeBillService feeBillService,
                                     PropertyDashboardService propertyDashboardService,
                                     UserService userService) {
        this.propertyUnitService = propertyUnitService;
        this.buildingService = buildingService;
        this.residentService = residentService;
        this.workOrderService = workOrderService;
        this.feeBillService = feeBillService;
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
                             @RequestParam(defaultValue = "1") int unitPage,
                             @RequestParam(defaultValue = "1") int residentPage,
                             @RequestParam(defaultValue = "1") int orderPage,
                             @RequestParam(defaultValue = "1") int billPage,
                             @RequestParam(defaultValue = "1") int userPage,
                             Model model) {
        String currentTab = normalizeTab(tab);
        model.addAttribute("currentTab", currentTab);

        var stats = propertyDashboardService.stats();
        model.addAttribute("stats", stats);
        model.addAttribute("recentOrders", propertyDashboardService.recentOrders(6));
        model.addAttribute("dueBills", propertyDashboardService.dueBills(6));
        model.addAttribute("overviewScaleMax", Math.max(1L,
                Math.max(Math.max(stats.getUnitCount(), stats.getResidentCount()),
                        Math.max(stats.getOpenOrderCount(), stats.getDueBillCount()))));
        model.addAttribute("collectionRate", computeCollectionRate(stats.getTotalReceived(), stats.getTotalReceivable()));

        model.addAttribute("unitPageResult", propertyUnitService.listPaged(unitKeyword, unitBuildingId, unitStatus, unitPage, DEFAULT_PAGE_SIZE));
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("unitKeyword", unitKeyword);
        model.addAttribute("unitBuildingId", unitBuildingId);
        model.addAttribute("unitStatus", unitStatus);

        model.addAttribute("residentPageResult", residentService.listPaged(residentKeyword, residentStatus, residentPage, DEFAULT_PAGE_SIZE));
        model.addAttribute("residentKeyword", residentKeyword);
        model.addAttribute("residentStatus", residentStatus);

        model.addAttribute("orderPageResult", workOrderService.listPaged(workOrderStatus, workOrderPriority, orderPage, DEFAULT_PAGE_SIZE));
        model.addAttribute("workOrderStatus", workOrderStatus);
        model.addAttribute("workOrderPriority", workOrderPriority);

        model.addAttribute("billPageResult", feeBillService.listPaged(billStatus, billBillingMonth, billPage, DEFAULT_PAGE_SIZE));
        model.addAttribute("billStatus", billStatus);
        model.addAttribute("billBillingMonth", billBillingMonth);

        model.addAttribute("userPageResult", userService.listByFiltersPaged(userQ, userRole, userStatus, userPage, DEFAULT_PAGE_SIZE));
        model.addAttribute("roles", Role.values());
        model.addAttribute("userQ", userQ);
        model.addAttribute("userRole", userRole);
        model.addAttribute("userStatus", userStatus);

        return "admin/management/index";
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

    private int computeCollectionRate(BigDecimal received, BigDecimal receivable) {
        if (receivable == null || received == null || receivable.signum() <= 0) {
            return 0;
        }
        BigDecimal rate = received.multiply(BigDecimal.valueOf(100))
                .divide(receivable, 0, RoundingMode.DOWN);
        int value = rate.intValue();
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
