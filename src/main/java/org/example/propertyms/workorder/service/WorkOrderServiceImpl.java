package org.example.propertyms.workorder.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.CodeGenerator;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.workorder.mapper.WorkOrderMapper;
import org.example.propertyms.workorder.model.Priority;
import org.example.propertyms.workorder.model.WorkOrder;
import org.example.propertyms.workorder.model.WorkOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {
    private static final Set<String> VALID_STATUSES = Set.of(
            WorkOrderStatus.OPEN.name(),
            WorkOrderStatus.IN_PROGRESS.name(),
            WorkOrderStatus.DONE.name(),
            WorkOrderStatus.CLOSED.name());

    private final WorkOrderMapper workOrderMapper;

    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper) {
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    public PageResult<WorkOrder> listPaged(String keyword, String status, String priority, int page, int pageSize) {
        long total = workOrderMapper.count(keyword, status, priority);
        int offset = PageResult.calcOffset(page, pageSize);
        List<WorkOrder> items = workOrderMapper.findAllPaged(keyword, status, priority, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    public List<WorkOrder> listAll(String keyword, String status, String priority) {
        return workOrderMapper.findAll(keyword, status, priority);
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
                || StringHelper.isBlank(workOrder.getResidentName())
                || StringHelper.isBlank(workOrder.getDescription())) {
            throw new IllegalArgumentException("请完整填写工单信息。");
        }
        workOrder.setOrderNo(CodeGenerator.nextWorkOrderNo());
        if (StringHelper.isBlank(workOrder.getPriority())) {
            workOrder.setPriority(Priority.MEDIUM.name());
        }
        workOrder.setStatus(WorkOrderStatus.OPEN.name());
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
        if (WorkOrderStatus.IN_PROGRESS.name().equals(targetStatus) && scheduledAt == null) {
            scheduledAt = LocalDateTime.now();
        }
        if (WorkOrderStatus.DONE.name().equals(targetStatus) || WorkOrderStatus.CLOSED.name().equals(targetStatus)) {
            finishedAt = LocalDateTime.now();
        }

        String finalAssignee = StringHelper.isBlank(assignee) ? existing.getAssignee() : assignee;
        workOrderMapper.updateStatus(id, targetStatus, finalAssignee, scheduledAt, finishedAt);
    }

    @Override
    public long countOpen() {
        return workOrderMapper.countOpen();
    }

    @Override
    public List<WorkOrder> findRecent(int limit) {
        return workOrderMapper.findRecent(limit);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase();
    }
}

