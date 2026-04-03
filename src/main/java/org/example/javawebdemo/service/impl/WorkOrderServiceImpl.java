package org.example.javawebdemo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.example.javawebdemo.mapper.WorkOrderMapper;
import org.example.javawebdemo.model.WorkOrder;
import org.example.javawebdemo.service.WorkOrderService;
import org.example.javawebdemo.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {
    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "DONE", "CLOSED");

    private final WorkOrderMapper workOrderMapper;

    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper) {
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    public List<WorkOrder> list(String status, String priority) {
        return workOrderMapper.findAll(status, priority);
    }

    @Override
    public WorkOrder findById(Long id) {
        return workOrderMapper.findById(id);
    }

    @Override
    @Transactional
    public void create(WorkOrder workOrder) {
        if (workOrder == null
                || workOrder.getUnitId() == null
                || isBlank(workOrder.getResidentName())
                || isBlank(workOrder.getDescription())) {
            throw new IllegalArgumentException("请完整填写工单信息。");
        }
        workOrder.setOrderNo(CodeGenerator.nextWorkOrderNo());
        if (isBlank(workOrder.getPriority())) {
            workOrder.setPriority("MEDIUM");
        }
        workOrder.setStatus("OPEN");
        workOrderMapper.insert(workOrder);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status, String assignee) {
        WorkOrder existing = workOrderMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("工单不存在。");
        }

        String targetStatus = normalizeStatus(status);
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("不支持的工单状态。");
        }

        LocalDateTime finishedAt = existing.getFinishedAt();
        LocalDateTime scheduledAt = existing.getScheduledAt();
        if ("IN_PROGRESS".equals(targetStatus) && scheduledAt == null) {
            scheduledAt = LocalDateTime.now();
        }
        if ("DONE".equals(targetStatus) || "CLOSED".equals(targetStatus)) {
            finishedAt = LocalDateTime.now();
        }

        String finalAssignee = isBlank(assignee) ? existing.getAssignee() : assignee;
        workOrderMapper.updateStatus(id, targetStatus, finalAssignee, scheduledAt, finishedAt);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
