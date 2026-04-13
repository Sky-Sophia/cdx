package org.example.propertyms.unit.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUnitControllerTest {

    @Mock
    private PropertyUnitService propertyUnitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUnitController adminUnitController = new AdminUnitController(propertyUnitService, mock(BuildingService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(adminUnitController).build();
    }

    @Test
    void delete_shouldRedirectWithErrorWhenUnitHasDependencies() throws Exception {
        doThrow(new IllegalArgumentException("该房屋仍有关联账单、住户或工单，暂时无法删除，请先清理关联数据后再试。"))
                .when(propertyUnitService)
                .deleteById(1L);

        mockMvc.perform(post("/admin/units/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/units/edit/1"))
                .andExpect(flash().attribute("error", "该房屋仍有关联账单、住户或工单，暂时无法删除，请先清理关联数据后再试。"));

        verify(propertyUnitService).deleteById(1L);
    }

    @Test
    void delete_shouldRedirectWithSuccessWhenDeleteSucceeds() throws Exception {
        mockMvc.perform(post("/admin/units/delete/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=units"))
                .andExpect(flash().attribute("success", "房屋已删除。"));

        verify(propertyUnitService).deleteById(2L);
    }
}



