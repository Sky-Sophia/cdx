package org.example.javawebdemo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.example.javawebdemo.mapper.WorkOrderMapper;
import org.example.javawebdemo.model.WorkOrder;
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

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @Test
    void create_shouldSetDefaultsAndPersist() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setUnitId(1L);
        workOrder.setResidentName("Alice");
        workOrder.setDescription("Water leakage");
        workOrder.setPriority(null);

        workOrderService.create(workOrder);

        assertEquals("OPEN", workOrder.getStatus());
        assertEquals("MEDIUM", workOrder.getPriority());
        assertNotNull(workOrder.getOrderNo());
        verify(workOrderMapper).insert(workOrder);
    }

    @Test
    void updateStatus_shouldAutoSetScheduleTimeWhenInProgress() {
        WorkOrder existing = new WorkOrder();
        existing.setId(5L);
        existing.setAssignee("Jack");
        when(workOrderMapper.findById(5L)).thenReturn(existing);

        workOrderService.updateStatus(5L, "IN_PROGRESS", " ");

        ArgumentCaptor<LocalDateTime> scheduledCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> finishedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(workOrderMapper).updateStatus(
                eq(5L),
                eq("IN_PROGRESS"),
                eq("Jack"),
                scheduledCaptor.capture(),
                finishedCaptor.capture());
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
        existing.setAssignee("Tom");
        when(workOrderMapper.findById(8L)).thenReturn(existing);

        workOrderService.updateStatus(8L, "DONE", "Jerry");

        ArgumentCaptor<LocalDateTime> scheduledCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> finishedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(workOrderMapper).updateStatus(
                eq(8L),
                eq("DONE"),
                eq("Jerry"),
                scheduledCaptor.capture(),
                finishedCaptor.capture());
        assertNotNull(finishedCaptor.getValue());
    }
}
