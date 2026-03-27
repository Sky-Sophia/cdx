package org.example.javawebdemo.controller;

import java.time.LocalDateTime;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.mapper.WorkOrderMapper;
import org.example.javawebdemo.model.WorkOrder;
import org.example.javawebdemo.util.CodeGenerator;
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
public class AdminShowController {
    private final WorkOrderMapper workOrderMapper;
    private final PropertyUnitMapper propertyUnitMapper;

    public AdminShowController(WorkOrderMapper workOrderMapper, PropertyUnitMapper propertyUnitMapper) {
        this.workOrderMapper = workOrderMapper;
        this.propertyUnitMapper = propertyUnitMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String priority,
                       Model model) {
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
        model.addAttribute("units", propertyUnitMapper.findAllSimple());
        return "admin/work-order-form";
    }

    @PostMapping("/save")
    public String save(WorkOrder workOrder, RedirectAttributes redirectAttributes) {
        if (workOrder.getUnitId() == null
                || workOrder.getResidentName() == null
                || workOrder.getResidentName().isBlank()
                || workOrder.getDescription() == null
                || workOrder.getDescription().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "请完整填写工单信息。");
            return "redirect:/admin/work-orders/new";
        }

        workOrder.setOrderNo(CodeGenerator.nextWorkOrderNo());
        if (workOrder.getPriority() == null || workOrder.getPriority().isBlank()) {
            workOrder.setPriority("MEDIUM");
        }
        workOrder.setStatus("OPEN");
        workOrderMapper.insert(workOrder);
        redirectAttributes.addFlashAttribute("success", "工单已创建。");
        return "redirect:/admin/management?tab=work-orders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String assignee,
                               RedirectAttributes redirectAttributes) {
        WorkOrder existing = workOrderMapper.findById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "工单不存在。");
            return "redirect:/admin/management?tab=work-orders";
        }

        LocalDateTime finishedAt = existing.getFinishedAt();
        LocalDateTime scheduledAt = existing.getScheduledAt();
        if ("IN_PROGRESS".equals(status) && scheduledAt == null) {
            scheduledAt = LocalDateTime.now();
        }
        if ("DONE".equals(status) || "CLOSED".equals(status)) {
            finishedAt = LocalDateTime.now();
        }

        String finalAssignee = (assignee == null || assignee.isBlank()) ? existing.getAssignee() : assignee;
        workOrderMapper.updateStatus(id, status, finalAssignee, scheduledAt, finishedAt);
        redirectAttributes.addFlashAttribute("success", "工单状态已更新。");
        return "redirect:/admin/management?tab=work-orders";
    }
}
