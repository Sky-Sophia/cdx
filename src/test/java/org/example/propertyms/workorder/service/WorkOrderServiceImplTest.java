package org.example.propertyms.workorder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.example.propertyms.employee.mapper.EmployeeMapper;
import org.example.propertyms.workorder.mapper.WorkOrderMapper;
import org.example.propertyms.workorder.model.WorkOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock
    private WorkOrderMapper workOrderMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @Test
    void create_shouldSetDefaultsAndPersist() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setUnitId(1L);
        workOrder.setDescription("Water leakage");
        workOrder.setPriority(null);
        when(workOrderMapper.findActiveResidentIdByUnitId(1L)).thenReturn(11L);

        workOrderService.create(workOrder);

        assertEquals("OPEN", workOrder.getStatus());
        assertEquals("MEDIUM", workOrder.getPriority());
        assertNotNull(workOrder.getOrderNo());
        assertEquals(11L, workOrder.getResidentId());
        verify(workOrderMapper).insert(workOrder);
    }

    @Test
    void updateStatus_shouldAutoSetScheduleTimeWhenInProgress() {
        WorkOrder existing = new WorkOrder();
        existing.setId(5L);
        existing.setAssigneeEmployeeId(12L);
        when(workOrderMapper.findById(5L)).thenReturn(existing);
        when(employeeMapper.findActiveEmployeeIdByAccountId(99L)).thenReturn(12L);

        workOrderService.updateStatus(5L, "IN_PROGRESS", 99L);

        ArgumentCaptor<LocalDateTime> scheduledCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> finishedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Long> assigneeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(workOrderMapper).updateStatus(
                eq(5L),
                eq("IN_PROGRESS"),
                assigneeCaptor.capture(),
                scheduledCaptor.capture(),
                finishedCaptor.capture());
        assertEquals(12L, assigneeCaptor.getValue());
        assertNotNull(scheduledCaptor.getValue());
        assertNull(finishedCaptor.getValue());
    }

    @Test
    void updateStatus_shouldRejectUnsupportedStatus() {
        WorkOrder existing = new WorkOrder();
        existing.setId(7L);
        when(workOrderMapper.findById(7L)).thenReturn(existing);

        assertThrows(
                IllegalArgumentException.class,
                () -> workOrderService.updateStatus(7L, "UNKNOWN", null));
    }

    @Test
    void updateStatus_shouldSetFinishTimeForDone() {
        WorkOrder existing = new WorkOrder();
        existing.setId(8L);
        existing.setAssigneeEmployeeId(21L);
        when(workOrderMapper.findById(8L)).thenReturn(existing);
        when(employeeMapper.findActiveEmployeeIdByAccountId(101L)).thenReturn(21L);

        workOrderService.updateStatus(8L, "DONE", 101L);

        ArgumentCaptor<LocalDateTime> scheduledCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> finishedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Long> assigneeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(workOrderMapper).updateStatus(
                eq(8L),
                eq("DONE"),
                assigneeCaptor.capture(),
                scheduledCaptor.capture(),
                finishedCaptor.capture());
        assertEquals(21L, assigneeCaptor.getValue());
        assertNotNull(finishedCaptor.getValue());
    }
}



