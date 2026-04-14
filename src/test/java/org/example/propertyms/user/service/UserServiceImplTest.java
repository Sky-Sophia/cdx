package org.example.propertyms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.example.propertyms.common.util.PasswordUtils;
import org.example.propertyms.user.mapper.UserMapper;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserDepartmentResolver userDepartmentResolver;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldCreateResidentAccountAndHashPassword() {
        when(userMapper.findByUsername("admin_user")).thenReturn(null);
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(Map.of("id", 100L));
            return 1;
        }).doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(Map.of("id", 200L));
            return 1;
        }).when(jdbcTemplate).update(any(), any(KeyHolder.class));

        User stored = new User();
        stored.setId(200L);
        stored.setUsername("admin_user");
        stored.setRole(Role.RESIDENT);
        stored.setStatus("ACTIVE");
        when(userMapper.findById(200L)).thenReturn(stored);

        User user = userService.register("Admin_User", "Admin123!@");

        assertEquals(200L, user.getId());
        assertEquals("admin_user", user.getUsername());
        verify(userMapper).findById(200L);
        assertNotNull(user);
    }

    @Test
    void authenticate_shouldMatchPlainTextPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(PasswordUtils.hash("Admin123!@"));
        user.setStatus("ACTIVE");
        when(userMapper.findByUsername("admin")).thenReturn(user);

        User authenticated = userService.authenticate(" admin ", "Admin123!@");

        assertEquals(user, authenticated);
    }

    @Test
    void authenticate_shouldRejectDisabledUserEvenWhenPasswordMatches() {
        User user = new User();
        user.setId(2L);
        user.setUsername("manager");
        user.setPassword(PasswordUtils.hash("Manager123!@"));
        user.setStatus("DISABLED");
        when(userMapper.findByUsername("manager")).thenReturn(user);

        User authenticated = userService.authenticate("manager", "Manager123!@");

        assertNull(authenticated);
    }

    @Test
    void resetPassword_shouldPersistHashedPassword() {
        User user = new User();
        user.setId(3L);
        when(userMapper.findById(3L)).thenReturn(user);

        userService.resetPassword(3L, "Reset123!@");

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(any(String.class), passwordCaptor.capture(), eq(3L));
        assertNotNull(passwordCaptor.getValue());
        assertTrue(PasswordUtils.matches("Reset123!@", passwordCaptor.getValue()));
    }

    @Test
    void updateManagementProfile_shouldIgnoreBlankStatus() {
        User user = new User();
        user.setId(5L);
        user.setUsername("engineer");
        user.setRole(Role.ENGINEER);
        user.setStatus("ACTIVE");
        when(userMapper.findById(5L)).thenReturn(user);

        userService.updateManagementProfile(5L, Role.ADMIN, "MANAGEMENT", " ");

        verifyNoInteractions(jdbcTemplate);
    }
}
