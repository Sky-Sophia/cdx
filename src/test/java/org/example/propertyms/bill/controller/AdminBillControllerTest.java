package org.example.propertyms.bill.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminBillControllerTest {

    @Mock
    private FeeBillService feeBillService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminBillController adminBillController = new AdminBillController(feeBillService, mock(PropertyUnitService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(adminBillController).build();
    }

    @Test
    void newForm_shouldRedirectToManagementAndOpenModal() throws Exception {
        mockMvc.perform(get("/admin/bills/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=bills"))
                .andExpect(flash().attribute("openCreateBillModal", true))
                .andExpect(flash().attributeExists("createBill"));
    }

    @Test
    void editForm_shouldLoadBillPaymentPage() throws Exception {
        FeeBill bill = new FeeBill();
        bill.setId(3L);
        bill.setBillNo("FB2026040001");
        bill.setStatus("PARTIAL");
        bill.setAmount(new BigDecimal("100.00"));
        bill.setPaidAmount(new BigDecimal("40.00"));
        when(feeBillService.findById(3L)).thenReturn(bill);

        mockMvc.perform(get("/admin/bills/edit/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bills/form"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("bill", bill));
    }

    @Test
    void markPaid_shouldRedirectBackToEditPageWhenPaymentFails() throws Exception {
        doThrow(new IllegalArgumentException("累计实收金额不能大于应收金额。"))
                .when(feeBillService)
                .recordPayment(5L, new BigDecimal("80.00"));

        mockMvc.perform(post("/admin/bills/5/pay").param("paidAmount", "80.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bills/edit/5"))
                .andExpect(flash().attribute("error", "累计实收金额不能大于应收金额。"));

        verify(feeBillService).recordPayment(5L, new BigDecimal("80.00"));
    }

    @Test
    void markPaid_shouldRedirectToBillListWhenPaymentSucceeds() throws Exception {
        mockMvc.perform(post("/admin/bills/8/pay").param("paidAmount", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=bills"))
                .andExpect(flash().attribute("success", "账单收款已登记。"));

        verify(feeBillService).recordPayment(8L, new BigDecimal("20.00"));
    }
}



