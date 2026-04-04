package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.model.WorkOrder;

public interface WorkOrderService {
    List<WorkOrder> list(String status, String priority);

    PageResult<WorkOrder> listPaged(String status, String priority, int page, int pageSize);

    WorkOrder findById(Long id);

    void create(WorkOrder workOrder);

    void updateStatus(Long id, String status, String assignee);
}
