package org.example.propertyms.workorder.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.propertyms.workorder.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminWorkOrderControllerTest {

    @Mock
    private WorkOrderService workOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminWorkOrderController controller = new AdminWorkOrderController(workOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void newForm_shouldRedirectToManagementAndOpenModal() throws Exception {
        mockMvc.perform(get("/admin/work-orders/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=work-orders"))
                .andExpect(flash().attribute("openCreateWorkOrderModal", true))
                .andExpect(flash().attributeExists("createWorkOrder"));
    }

    @Test
    void save_shouldReopenModalWhenCreateFails() throws Exception {
        doThrow(new IllegalArgumentException("该房屋当前没有在住住户，无法创建工单。"))
                .when(workOrderService)
                .create(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/admin/work-orders/save")
                        .param("unitId", "1")
                        .param("description", "水管漏水"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=work-orders"))
                .andExpect(flash().attribute("openCreateWorkOrderModal", true))
                .andExpect(flash().attributeExists("createWorkOrder"))
                .andExpect(flash().attribute("error", "该房屋当前没有在住住户，无法创建工单。"));

        verify(workOrderService).create(org.mockito.ArgumentMatchers.any());
    }
}
