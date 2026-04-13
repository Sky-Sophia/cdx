package org.example.propertyms.resident.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminResidentControllerTest {

    @Mock
    private ResidentService residentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminResidentController adminResidentController = new AdminResidentController(residentService, mock(PropertyUnitService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(adminResidentController).build();
    }

    @Test
    void newForm_shouldRedirectToManagementAndOpenModal() throws Exception {
        mockMvc.perform(get("/admin/residents/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=residents"))
                .andExpect(flash().attribute("openCreateResidentModal", true))
                .andExpect(flash().attributeExists("createResident"));
    }

    @Test
    void delete_shouldRedirectWithErrorWhenResidentHasDependencies() throws Exception {
        doThrow(new IllegalArgumentException("该住户仍有关联账单、工单或入住记录，暂时无法删除，请先处理关联数据后再试。"))
                .when(residentService)
                .deleteById(1L);

        mockMvc.perform(post("/admin/residents/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/residents/edit/1"))
                .andExpect(flash().attribute("error", "该住户仍有关联账单、工单或入住记录，暂时无法删除，请先处理关联数据后再试。"));

        verify(residentService).deleteById(1L);
    }

    @Test
    void delete_shouldRedirectWithSuccessWhenDeleteSucceeds() throws Exception {
        mockMvc.perform(post("/admin/residents/delete/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=residents"))
                .andExpect(flash().attribute("success", "住户已删除。"));

        verify(residentService).deleteById(2L);
    }
}



