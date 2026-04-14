package org.example.propertyms.template;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.building.model.Building;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.dashboard.dto.DashboardStats;
import org.example.propertyms.dashboard.service.PropertyDashboardService;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.model.PropertyUnit;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.user.service.DepartmentService;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.service.UserService;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminManagementTemplateRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyUnitService propertyUnitService;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private ResidentService residentService;

    @MockitoBean
    private WorkOrderService workOrderService;

    @MockitoBean
    private FeeBillService feeBillService;

    @MockitoBean
    private PropertyDashboardService propertyDashboardService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DepartmentService departmentService;

    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        adminSession = new MockHttpSession();
        adminSession.setAttribute(SessionKeys.CURRENT_USER, new UserSession(99L, "admin", Role.SUPER_ADMIN));

        DashboardStats stats = new DashboardStats();
        stats.setTotalReceived(BigDecimal.ZERO);
        stats.setTotalReceivable(BigDecimal.ZERO);
        when(propertyDashboardService.stats()).thenReturn(stats);
        when(propertyDashboardService.recentOrders(6)).thenReturn(List.of());
        when(propertyDashboardService.dueBills(6)).thenReturn(List.of());

        when(propertyUnitService.listPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        PropertyUnit simpleUnit = new PropertyUnit();
        simpleUnit.setId(1L);
        simpleUnit.setUnitNo("1-101");
        when(propertyUnitService.listSimple()).thenReturn(List.of(simpleUnit));

        Building building = new Building();
        building.setId(1L);
        building.setName("1号楼");
        when(buildingService.listAll()).thenReturn(List.of(building));
        when(departmentService.listEnabled()).thenReturn(List.of());

        when(residentService.listPaged(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        when(workOrderService.listPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 1, 9, 0));
        when(feeBillService.listPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        when(userService.listByFiltersPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
    }

    @Test
    void managementPage_shouldRenderCreateModalFragments() throws Exception {
        mockMvc.perform(get("/admin/management").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("residentCreateModalTitle")))
                .andExpect(content().string(containsString("workOrderCreateModalTitle")))
                .andExpect(content().string(containsString("billCreateModalTitle")))
                .andExpect(content().string(containsString("userCreateModalTitle")))
                .andExpect(content().string(containsString("RESIDENT RECORD REGISTRATION")))
                .andExpect(content().string(containsString("WORK ORDER REGISTRATION")))
                .andExpect(content().string(containsString("FEE BILL REGISTRATION")))
                .andExpect(content().string(containsString("USER PROFILE")))
                .andExpect(content().string(containsString("data-management-create-close")));
    }
}
