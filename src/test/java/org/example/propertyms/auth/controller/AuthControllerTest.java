package org.example.propertyms.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import lombok.Getter;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.auth.service.LoginRateLimiter;
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
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Getter
    @Mock
    private LoginRateLimiter rateLimiter;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void loginPage_shouldRedirectWhenAlreadyLoggedIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));

        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=dashboard"));
    }

    @Test
    void loginPage_shouldShowLoginFormWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_shouldKeepPanelOpenOnVerifyStepWhenUsernameMissing() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .param("username", " ")
                        .param("newPassword", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("tab", "login"))
                .andExpect(model().attribute("forgotPanelOpen", true))
                .andExpect(model().attribute("forgotStep", "verify"))
                .andExpect(model().attribute("error", "请输入用户名。"));
    }

    @Test
    void forgotPassword_shouldRedirectToLoginWhenResetSucceeds() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("resident01");
        when(userService.findByUsername("resident01")).thenReturn(user);

        mockMvc.perform(post("/forgot-password")
                        .param("username", "resident01")
                        .param("newPassword", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService).resetPassword(7L, "password123");
    }

}



