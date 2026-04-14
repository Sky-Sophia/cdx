package org.example.propertyms.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.service.NotificationService;
import org.example.propertyms.user.model.Role;
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
class NotificationApiControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationApiController notificationApiController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationApiController).build();
    }

    @Test
    void deleteAll_shouldReturnDeletedIdsAndUnreadCount() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(9L, "admin", Role.SUPER_ADMIN));
        when(notificationService.deleteAll(9L)).thenReturn(java.util.List.of(10L, 11L));
        when(notificationService.countUnread(9L)).thenReturn(3);

        mockMvc.perform(delete("/api/notifications").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2))
                .andExpect(jsonPath("$.unreadCount").value(3))
                .andExpect(jsonPath("$.ids[0]").value(10))
                .andExpect(jsonPath("$.ids[1]").value(11));

        verify(notificationService).deleteAll(9L);
    }

    @Test
    void hidePopup_shouldReturnUpdatedItem() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(9L, "admin", Role.SUPER_ADMIN));
        NotificationItem item = new NotificationItem();
        item.setId(12L);
        item.setPopupHidden(true);
        when(notificationService.hidePopup(9L, 12L)).thenReturn(item);
        when(notificationService.countUnread(9L)).thenReturn(1);

        mockMvc.perform(patch("/api/notifications/12/popup-hide").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.id").value(12))
                .andExpect(jsonPath("$.item.popupHidden").value(true))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void deleteOne_shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(delete("/api/notifications/5"))
                .andExpect(status().isUnauthorized());
    }
}
