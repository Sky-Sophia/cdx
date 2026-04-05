package org.example.propertyms.account.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.UserService;
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
class AccountControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
    }

    @Test
    void profile_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void profile_shouldRenderAccountProfileWhenLoggedIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.ADMIN));
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(Role.ADMIN);
        user.setStatus("ACTIVE");
        when(userService.findById(1L)).thenReturn(user);

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("userProfile", "roleLabel", "statusLabel"))
                .andExpect(view().name("account/profile"));

        verify(userService).findById(1L);
    }

    @Test
    void profile_shouldRedirectToLoginWhenSessionUserCannotBeLoaded() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.ADMIN));
        when(userService.findById(1L)).thenReturn(null);

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}

