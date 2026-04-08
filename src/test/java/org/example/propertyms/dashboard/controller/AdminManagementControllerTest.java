package org.example.propertyms.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.common.dto.PageResult;
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

	private void stubAllServices() {
		DashboardStats stats = new DashboardStats();
		stats.setTotalReceived(BigDecimal.ZERO);
		stats.setTotalReceivable(BigDecimal.ZERO);
		when(propertyDashboardService.stats()).thenReturn(stats);
		when(propertyDashboardService.recentOrders(6)).thenReturn(List.of());
		when(propertyDashboardService.dueBills(6)).thenReturn(List.of());
		when(propertyUnitService.listPaged(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResult<>(List.of(), 1, 10, 0));
		when(buildingService.listAll()).thenReturn(List.of());
		when(residentService.listPaged(any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResult<>(List.of(), 1, 10, 0));
		when(workOrderService.listPaged(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResult<>(List.of(), 1, 9, 0));
		when(feeBillService.listPaged(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResult<>(List.of(), 1, 10, 0));
		when(userService.listByFiltersPaged(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResult<>(List.of(), 1, 10, 0));
	}

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(adminManagementController).build();
	}

	@Test
	void management_workOrdersTab_shouldLoadAllData() throws Exception {
		stubAllServices();

		mockMvc.perform(get("/admin/management").param("tab", "work-orders"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/management/index"));

		// With full loading, all services are called for every tab
		org.mockito.Mockito.verify(workOrderService).listPaged(null, null, null, 1, 9);
		org.mockito.Mockito.verify(buildingService).listAll();
		org.mockito.Mockito.verify(propertyUnitService).listPaged(null, null, null, 1, 10);
	}

	@Test
	void management_dashboardTab_shouldLoadAllData() throws Exception {
		stubAllServices();

		mockMvc.perform(get("/admin/management").param("tab", "dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/management/index"));

		org.mockito.Mockito.verify(propertyDashboardService).recentOrders(6);
		org.mockito.Mockito.verify(propertyDashboardService).dueBills(6);
		org.mockito.Mockito.verify(buildingService).listAll();
		org.mockito.Mockito.verify(workOrderService).listPaged(null, null, null, 1, 9);
	}

	@Test
	void management_unitsTab_shouldExposeFilteredPaginationBaseUrl() throws Exception {
		stubAllServices();

		mockMvc.perform(get("/admin/management")
				.param("tab", "units")
				.param("unitKeyword", "A-101")
				.param("unitBuildingId", "3")
				.param("unitStatus", "OCCUPIED"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/management/index"))
				.andExpect(model().attribute("unitPaginationBaseUrl",
						"/admin/management?tab=units&unitKeyword=A-101&unitBuildingId=3&unitStatus=OCCUPIED"));
	}
}

