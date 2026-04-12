package org.example.propertyms.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.notification.mapper.NotificationAudienceMapper;
import org.example.propertyms.notification.mapper.NotificationMapper;
import org.example.propertyms.notification.model.NotificationDispatchResult;
import org.example.propertyms.notification.model.NotificationMessage;
import org.example.propertyms.notification.model.NotificationSendPayload;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationAudienceMapper notificationAudienceMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationMapper, notificationAudienceMapper);
    }

    @Test
    void send_singleShouldPersistNotification() {
        UserSession sender = new UserSession(1L, "admin", Role.OFFICE);
        User receiver = new User();
        receiver.setId(3L);
        receiver.setUsername("manager");
        receiver.setStatus("ACTIVE");

        NotificationSendPayload payload = new NotificationSendPayload();
        payload.setMsgType("通知");
        payload.setContent("请尽快核对四月账单。");
        payload.setTargetType("single");
        payload.setReceiver("3");

        when(notificationAudienceMapper.findActiveUserById(3L)).thenReturn(receiver);
        when(notificationMapper.insert(any(NotificationMessage.class))).thenAnswer(invocation -> {
            NotificationMessage message = invocation.getArgument(0);
            message.setId(11L);
            return 1;
        });

        List<NotificationDispatchResult> results = notificationService.send(sender, payload);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getReceiverId()).isEqualTo(3L);
        assertThat(results.getFirst().getItem().getSender()).isEqualTo("admin");
        assertThat(results.getFirst().getItem().getReceiver()).isEqualTo("manager");
        assertThat(results.getFirst().getItem().getTargetType()).isEqualTo("SINGLE");

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("请尽快核对四月账单。");
        assertThat(captor.getValue().getReceiverId()).isEqualTo(3L);
    }

    @Test
    void send_departmentShouldUseDepartmentRouting() {
        UserSession sender = new UserSession(1L, "admin", Role.OFFICE);
        User receiver = new User();
        receiver.setId(3L);
        receiver.setUsername("manager");
        receiver.setStatus("ACTIVE");

        NotificationSendPayload payload = new NotificationSendPayload();
        payload.setMsgType("提醒");
        payload.setContent("请跟进欠费住户。");
        payload.setTargetType("department");
        payload.setReceiver("管理部");

        when(notificationAudienceMapper.findActiveUsersByDepartment("MANAGEMENT", Role.MANAGEMENT, 1L))
                .thenReturn(List.of(receiver));
        when(notificationMapper.insert(any(NotificationMessage.class))).thenAnswer(invocation -> {
            NotificationMessage message = invocation.getArgument(0);
            message.setId(21L);
            return 1;
        });

        List<NotificationDispatchResult> results = notificationService.send(sender, payload);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getItem().getTargetType()).isEqualTo("DEPARTMENT");
        verify(notificationAudienceMapper).findActiveUsersByDepartment("MANAGEMENT", Role.MANAGEMENT, 1L);
    }

    @Test
    void markAllRead_shouldReturnUnreadIds() {
        when(notificationMapper.findUnreadIds(5L)).thenReturn(List.of(100L, 101L));

        List<Long> ids = notificationService.markAllRead(5L);

        assertThat(ids).containsExactly(100L, 101L);
        verify(notificationMapper).markAllRead(5L);
    }

    @Test
    void deleteAll_shouldHardDeleteInboxMessages() {
        when(notificationMapper.findInboxIds(5L)).thenReturn(List.of(100L, 101L, 102L));

        List<Long> ids = notificationService.deleteAll(5L);

        assertThat(ids).containsExactly(100L, 101L, 102L);
        verify(notificationMapper).deleteAll(5L);
    }

    @Test
    void send_shouldRejectBlankContent() {
        UserSession sender = new UserSession(1L, "admin", Role.OFFICE);
        NotificationSendPayload payload = new NotificationSendPayload();
        payload.setMsgType("通知");
        payload.setContent("   ");
        payload.setTargetType("all");

        assertThatThrownBy(() -> notificationService.send(sender, payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("消息内容不能为空");
    }
}
