package org.example.propertyms.workorder.service;

import java.util.List;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.workorder.model.WorkOrder;

public interface WorkOrderService {
    PageResult<WorkOrder> listPaged(String keyword, String status, String priority, int page, int pageSize);

    List<WorkOrder> listAll(String keyword, String status, String priority);

    WorkOrder findById(Long id);

    void create(WorkOrder workOrder);

    void updateStatus(Long id, String status, String assignee);

    long countOpen();

    List<WorkOrder> findRecent(int limit);
}


