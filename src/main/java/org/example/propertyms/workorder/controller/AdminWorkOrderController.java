package org.example.propertyms.workorder.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.common.util.ExcelExportHelper;
import org.example.propertyms.common.web.ManagementPageRouter;
import org.example.propertyms.workorder.model.WorkOrder;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/work-orders")
public class AdminWorkOrderController {
    private final WorkOrderService workOrderService;

    public AdminWorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String priority) {
        return ManagementPageRouter.redirectToTab("work-orders", builder -> {
            ManagementPageRouter.addTrimmedParam(builder, "workOrderKeyword", keyword);
            ManagementPageRouter.addTrimmedParam(builder, "workOrderStatus", status);
            ManagementPageRouter.addTrimmedParam(builder, "workOrderPriority", priority);
        });
    }

    @GetMapping("/new")
    public String newForm(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("createWorkOrder", defaultCreateWorkOrder());
        redirectAttributes.addFlashAttribute("openCreateWorkOrderModal", true);
        return RedirectUrls.MANAGEMENT_WORK_ORDERS;
    }

    @PostMapping("/save")
    public String save(WorkOrder workOrder, RedirectAttributes redirectAttributes) {
        try {
            workOrderService.create(workOrder);
            redirectAttributes.addFlashAttribute("success", "工单已创建。");
            return RedirectUrls.MANAGEMENT_WORK_ORDERS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            prepareCreateModalState(redirectAttributes, workOrder);
            return RedirectUrls.MANAGEMENT_WORK_ORDERS;
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            UserSession currentUser = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
            workOrderService.updateStatus(id, status, currentUser != null ? currentUser.getId() : null);
            redirectAttributes.addFlashAttribute("success", "工单状态已更新。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return RedirectUrls.MANAGEMENT_WORK_ORDERS;
    }

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String priority,
                            HttpServletResponse response) throws IOException {
        var list = workOrderService.listAll(keyword, status, priority);
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] headers = {"工单号", "房号", "报修人", "联系电话", "类别", "优先级", "处理人", "状态", "创建时间"};
        ExcelExportHelper.export(response, "工单列表", "工单列表", headers, list, (row, w) -> {
            row.getCell(0).setCellValue(w.getOrderNo() != null ? w.getOrderNo() : "");
            row.getCell(1).setCellValue(w.getUnitNo() != null ? w.getUnitNo() : "");
            row.getCell(2).setCellValue(w.getResidentName() != null ? w.getResidentName() : "");
            row.getCell(3).setCellValue(w.getPhone() != null ? w.getPhone() : "");
            row.getCell(4).setCellValue(w.getCategory() != null ? w.getCategory() : "");
            row.getCell(5).setCellValue(priorityLabel(w.getPriority()));
            row.getCell(6).setCellValue(w.getAssignee() != null ? w.getAssignee() : "");
            row.getCell(7).setCellValue(workOrderStatusLabel(w.getStatus()));
            row.getCell(8).setCellValue(w.getCreatedAt() != null ? w.getCreatedAt().format(dtFmt) : "");
        });
    }

    private WorkOrder defaultCreateWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setPriority("MEDIUM");
        workOrder.setStatus("OPEN");
        workOrder.setCategory("水电维修");
        return workOrder;
    }

    private void prepareCreateModalState(RedirectAttributes redirectAttributes, WorkOrder workOrder) {
        redirectAttributes.addFlashAttribute("createWorkOrder", workOrder);
        redirectAttributes.addFlashAttribute("openCreateWorkOrderModal", true);
    }

    private String priorityLabel(String priority) {
        if (priority == null) {
            return "";
        }
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }

    private String workOrderStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "OPEN" -> "待受理";
            case "IN_PROGRESS" -> "处理中";
            case "DONE" -> "已完成";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
