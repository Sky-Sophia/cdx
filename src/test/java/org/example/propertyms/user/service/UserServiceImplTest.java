package org.example.propertyms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.propertyms.user.mapper.UserMapper;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldStorePlainTextPassword() {
        when(userMapper.findByUsername("admin_user")).thenReturn(null);

        User user = userService.register("Admin_User", "Admin123!@");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("admin_user", savedUser.getUsername());
        assertEquals("Admin123!@", savedUser.getPassword());
        assertEquals(Role.RESIDENT, savedUser.getRole());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertNotNull(user);
    }

    @Test
    void authenticate_shouldMatchPlainTextPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("Admin123!@");
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
        user.setPassword("Manager123!@");
        user.setStatus("DISABLED");
        when(userMapper.findByUsername("manager")).thenReturn(user);

        User authenticated = userService.authenticate("manager", "Manager123!@");

        assertNull(authenticated);
    }

    @Test
    void resetPassword_shouldPersistPlainTextPassword() {
        User user = new User();
        user.setId(3L);
        when(userMapper.findById(3L)).thenReturn(user);

        userService.resetPassword(3L, "Reset123!@");

        verify(userMapper).updatePassword(eq(3L), eq("Reset123!@"));
    }
}



