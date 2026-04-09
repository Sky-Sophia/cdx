package org.example.propertyms.template;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.building.model.Building;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.resident.model.Resident;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.model.PropertyUnit;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFormTemplateRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyUnitService propertyUnitService;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private ResidentService residentService;

    @MockBean
    private FeeBillService feeBillService;

    @MockBean
    private UserService userService;

    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        adminSession = new MockHttpSession();
        adminSession.setAttribute(SessionKeys.CURRENT_USER, new UserSession(99L, "admin", Role.ADMIN));

        PropertyUnit simpleUnit = new PropertyUnit();
        simpleUnit.setId(1L);
        simpleUnit.setUnitNo("1-101");
        simpleUnit.setBuildingId(1L);
        simpleUnit.setBuildingName("1号楼");
        simpleUnit.setOccupancyStatus("OCCUPIED");
        when(propertyUnitService.listSimple()).thenReturn(List.of(simpleUnit));

        Building building = new Building();
        building.setId(1L);
        building.setName("1号楼");
        when(buildingService.listAll()).thenReturn(List.of(building));
    }

    @Test
    void residentEditForm_shouldRenderSuccessfully() throws Exception {
        Resident resident = new Resident();
        resident.setId(1L);
        resident.setUnitId(1L);
        resident.setName("张三");
        resident.setPhone("13800000000");
        resident.setIdentityNo("110101199001010011");
        resident.setResidentType("OWNER");
        resident.setStatus("ACTIVE");
        resident.setMoveInDate(LocalDate.of(2024, 1, 1));
        resident.setUpdatedAt(LocalDateTime.of(2026, 4, 9, 11, 0));
        when(residentService.findById(1L)).thenReturn(resident);

        mockMvc.perform(get("/admin/residents/edit/1").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("住户档案登记表")));
    }

    @Test
    void billEditForm_shouldRenderSuccessfully() throws Exception {
        FeeBill bill = new FeeBill();
        bill.setId(2L);
        bill.setBillNo("FB2026040002");
        bill.setUnitId(1L);
        bill.setUnitNo("1-101");
        bill.setBillingMonth("2026-04");
        bill.setAmount(new BigDecimal("100.00"));
        bill.setPaidAmount(new BigDecimal("40.00"));
        bill.setStatus("PARTIAL");
        bill.setDueDate(LocalDate.of(2026, 4, 30));
        bill.setRemarks("测试账单");
        bill.setUpdatedAt(LocalDateTime.of(2026, 4, 9, 11, 0));
        when(feeBillService.findById(2L)).thenReturn(bill);

        mockMvc.perform(get("/admin/bills/edit/2").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("费用账单登记表")));
    }

    @Test
    void userEditForm_shouldRenderSuccessfully() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setUsername("finance_user");
        user.setRole(Role.FINANCE);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 4, 9, 10, 30));
        when(userService.findById(3L)).thenReturn(user);

        mockMvc.perform(get("/admin/users/edit/3").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统用户档案")));
    }

    @Test
    void unitEditForm_shouldRenderSuccessfully() throws Exception {
        PropertyUnit unit = new PropertyUnit();
        unit.setId(4L);
        unit.setBuildingId(1L);
        unit.setBuildingName("1号楼");
        unit.setUnitNo("1-201");
        unit.setFloorNo(2);
        unit.setAreaM2(new BigDecimal("89.50"));
        unit.setOwnerName("李四");
        unit.setOwnerPhone("13900000000");
        unit.setOccupancyStatus("OCCUPIED");
        unit.setUpdatedAt(LocalDateTime.of(2026, 4, 9, 11, 20));
        when(propertyUnitService.findById(4L)).thenReturn(unit);

        mockMvc.perform(get("/admin/units/edit/4").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("房屋档案登记表")));
    }
}

