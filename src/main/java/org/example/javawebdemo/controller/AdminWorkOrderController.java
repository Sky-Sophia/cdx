package org.example.javawebdemo.controller;

import org.example.javawebdemo.model.WorkOrder;
import org.example.javawebdemo.service.PropertyUnitService;
import org.example.javawebdemo.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/work-orders")
public class AdminWorkOrderController {
    private final WorkOrderService workOrderService;
    private final PropertyUnitService propertyUnitService;

    public AdminWorkOrderController(WorkOrderService workOrderService, PropertyUnitService propertyUnitService) {
        this.workOrderService = workOrderService;
        this.propertyUnitService = propertyUnitService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String priority) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "work-orders");
        if (status != null && !status.isBlank()) {
            builder.queryParam("workOrderStatus", status);
        }
        if (priority != null && !priority.isBlank()) {
            builder.queryParam("workOrderPriority", priority);
        }
        return "redirect:" + builder.toUriString();
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setPriority("MEDIUM");
        workOrder.setStatus("OPEN");
        model.addAttribute("workOrder", workOrder);
        model.addAttribute("units", propertyUnitService.listSimple());
        return "admin/work-order-form";
    }

    @PostMapping("/save")
    public String save(WorkOrder workOrder, RedirectAttributes redirectAttributes) {
        try {
            workOrderService.create(workOrder);
            redirectAttributes.addFlashAttribute("success", "工单已创建。");
            return "redirect:/admin/management?tab=work-orders";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/work-orders/new";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String assignee,
                               RedirectAttributes redirectAttributes) {
        try {
            workOrderService.updateStatus(id, status, assignee);
            redirectAttributes.addFlashAttribute("success", "工单状态已更新。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/management?tab=work-orders";
    }
}
