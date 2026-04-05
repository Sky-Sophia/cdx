package org.example.propertyms.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.dashboard.dto.DashboardStats;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyDashboardServiceTest {

    @Mock
    private BuildingService buildingService;

    @Mock
    private PropertyUnitService propertyUnitService;

    @Mock
    private ResidentService residentService;

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private FeeBillService feeBillService;

    @InjectMocks
    private PropertyDashboardService propertyDashboardService;

    @Test
    void stats_shouldUseDistinctOccupiedUnitsForOccupancyInputs() {
        when(buildingService.countAll()).thenReturn(12L);
        when(propertyUnitService.countAll()).thenReturn(240L);
        when(residentService.countOccupiedUnits()).thenReturn(180L);
        when(residentService.countActive()).thenReturn(328L);
        when(workOrderService.countOpen()).thenReturn(15L);
        when(feeBillService.countDue()).thenReturn(30L);
        when(feeBillService.sumReceivable()).thenReturn(new BigDecimal("45678.90"));
        when(feeBillService.sumReceived()).thenReturn(new BigDecimal("34567.89"));

        DashboardStats stats = propertyDashboardService.stats();

        assertEquals(240L, stats.getUnitCount());
        assertEquals(180L, stats.getOccupiedUnitCount());
        assertEquals(328L, stats.getResidentCount());
        assertEquals(new BigDecimal("45678.90"), stats.getTotalReceivable());
        assertEquals(new BigDecimal("34567.89"), stats.getTotalReceived());
    }

    @Test
    void stats_shouldDefaultMoneyTotalsToZeroWhenServiceReturnsZero() {
        when(buildingService.countAll()).thenReturn(0L);
        when(propertyUnitService.countAll()).thenReturn(0L);
        when(residentService.countOccupiedUnits()).thenReturn(0L);
        when(residentService.countActive()).thenReturn(0L);
        when(workOrderService.countOpen()).thenReturn(0L);
        when(feeBillService.countDue()).thenReturn(0L);
        when(feeBillService.sumReceivable()).thenReturn(BigDecimal.ZERO);
        when(feeBillService.sumReceived()).thenReturn(BigDecimal.ZERO);

        DashboardStats stats = propertyDashboardService.stats();

        assertEquals(BigDecimal.ZERO, stats.getTotalReceivable());
        assertEquals(BigDecimal.ZERO, stats.getTotalReceived());
    }
}

