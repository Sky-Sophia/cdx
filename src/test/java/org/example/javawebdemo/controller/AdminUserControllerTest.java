package org.example.javawebdemo.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
    }

    @Test
    void list_shouldRejectNonAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "staff", Role.STAFF));

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=dashboard"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateStatus_shouldPreventSelfDisable() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(10L, "admin", Role.ADMIN));

        mockMvc.perform(post("/admin/users/10/status")
                        .session(session)
                        .param("status", "DISABLED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=users"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, never()).updateStatus(10L, "DISABLED");
    }

    @Test
    void updateStatus_shouldUpdateWhenValid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.ADMIN));

        mockMvc.perform(post("/admin/users/2/status")
                        .session(session)
                        .param("status", "active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).updateStatus(2L, "ACTIVE");
    }
}
