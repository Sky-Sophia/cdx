package org.example.propertyms.template;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.service.NotificationService;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.service.DepartmentService;
import org.example.propertyms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AccountProfileTemplateRenderingTest.MockServicesConfig.class)
class AccountProfileTemplateRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private DepartmentService departmentService;

    private MockHttpSession profileSession;

    @BeforeEach
    void setUp() {
        profileSession = new MockHttpSession();
        profileSession.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "admin", Role.SUPER_ADMIN));

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(Role.SUPER_ADMIN);
        user.setDepartmentCode("OFFICE");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 4, 11, 10, 30));
        when(userService.findById(1L)).thenReturn(user);

        NotificationItem notificationItem = new NotificationItem();
        notificationItem.setId(101L);
        notificationItem.setMsgType("公告");
        notificationItem.setSender("系统管理员");
        notificationItem.setContent("本周物业服务安排已更新，请及时查看。");
        notificationItem.setSendTime(LocalDateTime.of(2026, 4, 11, 9, 25));
        notificationItem.setRead(false);
        when(notificationService.loadInbox(1L, 100)).thenReturn(List.of(notificationItem));
        when(notificationService.countUnread(1L)).thenReturn(1);
        when(buildingService.listAll()).thenReturn(List.of());
        when(departmentService.listEnabled()).thenReturn(List.of());
    }

    @Test
    void profile_shouldRenderHistoryModalSuccessfully() throws Exception {
        mockMvc.perform(get("/profile").session(profileSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("历史消息")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("所属部门")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("综合办公室")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("系统管理员")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("全部删除")));
    }

    @Test
    void profile_shouldShowResidentWhenUserHasNoDepartment() throws Exception {
        profileSession.setAttribute(SessionKeys.CURRENT_USER, new UserSession(2L, "resident", Role.RESIDENT));

        User user = new User();
        user.setId(2L);
        user.setUsername("resident");
        user.setRole(Role.RESIDENT);
        user.setStatus("ACTIVE");
        when(userService.findById(2L)).thenReturn(user);
        when(notificationService.loadInbox(2L, 100)).thenReturn(List.of());
        when(notificationService.countUnread(2L)).thenReturn(0);

        mockMvc.perform(get("/profile").session(profileSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("所属部门")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("普通住户")));
    }

    @TestConfiguration
    static class MockServicesConfig {
        @Bean
        @Primary
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        @Primary
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }

        @Bean
        @Primary
        BuildingService buildingService() {
            return mock(BuildingService.class);
        }

        @Bean
        @Primary
        DepartmentService departmentService() {
            return mock(DepartmentService.class);
        }
    }
}
