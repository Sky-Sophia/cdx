package org.example.propertyms.dashboard.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.web.ManagementPageRouter;
import org.example.propertyms.dashboard.service.PropertyDashboardService;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.example.propertyms.resident.model.Resident;
import org.example.propertyms.resident.model.ResidentType;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
import org.example.propertyms.workorder.model.WorkOrder;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 管理后台统一入口，集中装配各个页签所需数据。
 */
@Controller
@RequestMapping("/admin")
public class AdminManagementController {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int WORK_ORDER_PAGE_SIZE = 9;

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
                             @RequestParam(required = false) String workOrderKeyword,
                             @RequestParam(required = false) String workOrderStatus,
                             @RequestParam(required = false) String workOrderPriority,
                             @RequestParam(required = false) String billKeyword,
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

        loadDashboardData(model);
        loadUnitData(model, unitKeyword, unitBuildingId, unitStatus, unitPage);
        loadResidentData(model, residentKeyword, residentStatus, residentPage);
        loadWorkOrderData(model, workOrderKeyword, workOrderStatus, workOrderPriority, orderPage);
        loadBillData(model, billKeyword, billStatus, billBillingMonth, billPage);
        loadUserData(model, userQ, userRole, userStatus, userPage);
        model.addAttribute("unitOptions", propertyUnitService.listSimple());

        return "admin/management/index";
    }

    private void loadDashboardData(Model model) {
        var stats = propertyDashboardService.stats();
        model.addAttribute("stats", stats);
        model.addAttribute("collectionRate", computeCollectionRate(stats.getTotalReceived(), stats.getTotalReceivable()));
        model.addAttribute("recentOrders", propertyDashboardService.recentOrders(6));
        model.addAttribute("dueBills", propertyDashboardService.dueBills(6));
    }

    private void loadUnitData(Model model,
                              String keyword,
                              Long buildingId,
                              String status,
                              int page) {
        model.addAttribute("unitPageResult",
                propertyUnitService.listPaged(keyword, buildingId, status, page, DEFAULT_PAGE_SIZE));
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("unitKeyword", keyword);
        model.addAttribute("unitBuildingId", buildingId);
        model.addAttribute("unitStatus", status);
        model.addAttribute("unitPaginationBaseUrl", buildUnitPaginationBaseUrl(keyword, buildingId, status));
    }

    private void loadResidentData(Model model,
                                  String keyword,
                                  String status,
                                  int page) {
        model.addAttribute("residentPageResult",
                residentService.listPaged(keyword, status, page, DEFAULT_PAGE_SIZE));
        model.addAttribute("residentKeyword", keyword);
        model.addAttribute("residentStatus", status);
        model.addAttribute("residentPaginationBaseUrl", buildResidentPaginationBaseUrl(keyword, status));
        model.addAttribute("residentTypes", ResidentType.values());
        initializeCreateModal(model, "createResident", this::defaultCreateResident, "openCreateResidentModal");
    }

    private void loadWorkOrderData(Model model,
                                   String keyword,
                                   String status,
                                   String priority,
                                   int page) {
        model.addAttribute("orderPageResult",
                workOrderService.listPaged(keyword, status, priority, page, WORK_ORDER_PAGE_SIZE));
        model.addAttribute("workOrderKeyword", keyword);
        model.addAttribute("workOrderStatus", status);
        model.addAttribute("workOrderPriority", priority);
        model.addAttribute("workOrderPaginationBaseUrl", buildWorkOrderPaginationBaseUrl(keyword, status, priority));
        initializeCreateModal(model, "createWorkOrder", this::defaultCreateWorkOrder, "openCreateWorkOrderModal");
    }

    private void loadBillData(Model model,
                              String keyword,
                              String status,
                              String billingMonth,
                              int page) {
        model.addAttribute("billPageResult",
                feeBillService.listPaged(keyword, status, billingMonth, page, DEFAULT_PAGE_SIZE));
        model.addAttribute("billKeyword", keyword);
        model.addAttribute("billStatus", status);
        model.addAttribute("billBillingMonth", billingMonth);
        model.addAttribute("billPaginationBaseUrl", buildBillPaginationBaseUrl(keyword, status, billingMonth));
        initializeCreateModal(model, "createBill", this::defaultCreateBill, "openCreateBillModal");
    }

    private void loadUserData(Model model,
                              String keyword,
                              Role role,
                              String status,
                              int page) {
        model.addAttribute("userPageResult",
                userService.listByFiltersPaged(keyword, role, status, page, DEFAULT_PAGE_SIZE));
        model.addAttribute("roles", Role.values());
        model.addAttribute("userQ", keyword);
        model.addAttribute("userRole", role);
        model.addAttribute("userStatus", status);
        model.addAttribute("userPaginationBaseUrl", buildUserPaginationBaseUrl(keyword, role, status));
        initializeCreateModal(model, "createUser", this::defaultCreateUser, "openCreateUserModal");
    }

    private Resident defaultCreateResident() {
        Resident resident = new Resident();
        resident.setResidentType(ResidentType.OWNER.name());
        resident.setStatus("ACTIVE");
        return resident;
    }

    private WorkOrder defaultCreateWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setPriority("MEDIUM");
        workOrder.setStatus("OPEN");
        workOrder.setCategory("水电维修");
        return workOrder;
    }

    private FeeBill defaultCreateBill() {
        FeeBill bill = new FeeBill();
        bill.setStatus("UNPAID");
        bill.setPaidAmount(BigDecimal.ZERO);
        return bill;
    }

    private User defaultCreateUser() {
        User user = new User();
        user.setRole(Role.ADMIN);
        user.setDepartmentCode(NotificationDepartment.defaultForRole(Role.ADMIN).getCode());
        user.setStatus("ACTIVE");
        return user;
    }

    private String buildUnitPaginationBaseUrl(String keyword, Long buildingId, String status) {
        return ManagementPageRouter.buildTabUrl("units", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "unitKeyword", keyword);
            ManagementPageRouter.addParam(builder, "unitBuildingId", buildingId);
            ManagementPageRouter.addTrimmedParam(builder, "unitStatus", status);
        });
    }

    private String buildResidentPaginationBaseUrl(String keyword, String status) {
        return ManagementPageRouter.buildTabUrl("residents", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "residentKeyword", keyword);
            ManagementPageRouter.addTrimmedParam(builder, "residentStatus", status);
        });
    }

    private String buildWorkOrderPaginationBaseUrl(String keyword, String status, String priority) {
        return ManagementPageRouter.buildTabUrl("work-orders", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "workOrderKeyword", keyword);
            ManagementPageRouter.addTrimmedParam(builder, "workOrderStatus", status);
            ManagementPageRouter.addTrimmedParam(builder, "workOrderPriority", priority);
        });
    }

    private String buildBillPaginationBaseUrl(String keyword, String status, String billingMonth) {
        return ManagementPageRouter.buildTabUrl("bills", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "billKeyword", keyword);
            ManagementPageRouter.addTrimmedParam(builder, "billStatus", status);
            ManagementPageRouter.addTrimmedParam(builder, "billBillingMonth", billingMonth);
        });
    }

    private String buildUserPaginationBaseUrl(String keyword, Role role, String status) {
        return ManagementPageRouter.buildTabUrl("users", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "userQ", keyword);
            if (role != null) {
                builder.queryParam("userRole", role.name());
            }
            ManagementPageRouter.addTrimmedParam(builder, "userStatus", status);
        });
    }

    private String normalizeTab(String tab) {
        return switch (tab) {
            case "dashboard", "units", "residents", "work-orders", "complaints", "bills", "users" -> tab;
            default -> "dashboard";
        };
    }

    private void initializeCreateModal(Model model,
                                       String formAttributeName,
                                       Supplier<?> formValueSupplier,
                                       String openFlagAttributeName) {
        addModelAttributeIfAbsent(model, formAttributeName, formValueSupplier.get());
        addModelAttributeIfAbsent(model, openFlagAttributeName, false);
    }

    private void addModelAttributeIfAbsent(Model model, String attributeName, Object value) {
        if (!model.containsAttribute(attributeName)) {
            model.addAttribute(attributeName, value);
        }
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
