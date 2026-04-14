package org.example.propertyms.user.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.DepartmentService;
import org.example.propertyms.user.service.UserDepartmentResolver;
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
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private UserDepartmentResolver userDepartmentResolver;

    @InjectMocks
    private AdminUserController adminUserController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
    }

    @Test
    void list_shouldRejectNonSuperAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "manager", Role.ADMIN));

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=dashboard"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void updateStatus_shouldPreventSelfDisable() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(10L, "admin", Role.SUPER_ADMIN));

        User user = new User();
        user.setId(10L);
        when(userService.findById(10L)).thenReturn(user);

        mockMvc.perform(post("/admin/users/10/manage")
                        .session(session)
                        .param("role", "SUPER_ADMIN")
                        .param("departmentCode", "OFFICE")
                        .param("status", "DISABLED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/edit/10"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, never()).updateManagementProfile(10L, Role.SUPER_ADMIN, "OFFICE", "DISABLED");
    }

    @Test
    void editForm_shouldLoadUserManagementPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));

        User user = new User();
        user.setId(2L);
        user.setUsername("manager");
        user.setRole(Role.ADMIN);
        user.setStatus("ACTIVE");
        when(userService.findById(2L)).thenReturn(user);
        when(userDepartmentResolver.defaultDepartmentCode(Role.ADMIN)).thenReturn("MANAGEMENT");

        mockMvc.perform(get("/admin/users/edit/2").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attribute("editing", true))
                .andExpect(model().attribute("managedUser", user));
    }

    @Test
    void newForm_shouldRedirectToManagementAndOpenModal() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));
        when(userDepartmentResolver.defaultDepartmentCode(Role.ADMIN)).thenReturn("MANAGEMENT");

        mockMvc.perform(get("/admin/users/new").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=users"))
                .andExpect(flash().attribute("openCreateUserModal", true))
                .andExpect(flash().attributeExists("createUser"));
    }

    @Test
    void manage_shouldUpdateRoleAndStatusWhenValid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));

        User user = new User();
        user.setId(2L);
        user.setUsername("engineer");
        user.setRole(Role.ENGINEER);
        user.setStatus("ACTIVE");
        when(userService.findById(2L)).thenReturn(user);

        mockMvc.perform(post("/admin/users/2/manage")
                        .session(session)
                        .param("role", "ADMIN")
                        .param("departmentCode", "MANAGEMENT")
                        .param("status", "active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?tab=users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).updateManagementProfile(2L, Role.ADMIN, "MANAGEMENT", "active");
    }

    @Test
    void manage_shouldReturnToEditPageWhenDepartmentInvalid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));

        User user = new User();
        user.setId(2L);
        user.setRole(Role.ADMIN);
        when(userService.findById(2L)).thenReturn(user);
        doThrow(new IllegalArgumentException("所选部门不存在或已停用。"))
                .when(userService)
                .updateManagementProfile(2L, Role.ADMIN, "INVALID", "ACTIVE");

        mockMvc.perform(post("/admin/users/2/manage")
                        .session(session)
                        .param("role", "ADMIN")
                        .param("departmentCode", "INVALID")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/edit/2"))
                .andExpect(flash().attributeExists("error"));
    }
}
