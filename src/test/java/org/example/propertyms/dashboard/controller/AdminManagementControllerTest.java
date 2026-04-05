package org.example.propertyms.dashboard.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.dashboard.dto.DashboardStats;
import org.example.propertyms.dashboard.service.PropertyDashboardService;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.user.service.UserService;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminManagementControllerTest {

	@Mock private PropertyUnitService propertyUnitService;
	@Mock private BuildingService buildingService;
	@Mock private ResidentService residentService;
	@Mock private WorkOrderService workOrderService;
	@Mock private FeeBillService feeBillService;
	@Mock private PropertyDashboardService propertyDashboardService;
	@Mock private UserService userService;

	@InjectMocks
	private AdminManagementController adminManagementController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(adminManagementController).build();
	}

	@Test
	void management_workOrdersTab_shouldOnlyLoadWorkOrderData() throws Exception {
		DashboardStats stats = new DashboardStats();
		stats.setTotalReceived(BigDecimal.ZERO);
		stats.setTotalReceivable(BigDecimal.ZERO);
		when(propertyDashboardService.stats()).thenReturn(stats);

		mockMvc.perform(get("/admin/management").param("tab", "work-orders"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/management/index"));

		// With lazy loading, only workOrderService.listPaged should be called (not all services)
		org.mockito.Mockito.verify(workOrderService).listPaged(null, null, 1, 9);
		org.mockito.Mockito.verifyNoInteractions(buildingService);
	}

	@Test
	void management_dashboardTab_shouldLoadDashboardData() throws Exception {
		DashboardStats stats = new DashboardStats();
		stats.setTotalReceived(BigDecimal.ZERO);
		stats.setTotalReceivable(BigDecimal.ZERO);
		when(propertyDashboardService.stats()).thenReturn(stats);
		when(propertyDashboardService.recentOrders(6)).thenReturn(List.of());
		when(propertyDashboardService.dueBills(6)).thenReturn(List.of());

		mockMvc.perform(get("/admin/management").param("tab", "dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/management/index"));

		org.mockito.Mockito.verify(propertyDashboardService).recentOrders(6);
		org.mockito.Mockito.verify(propertyDashboardService).dueBills(6);
	}
}

