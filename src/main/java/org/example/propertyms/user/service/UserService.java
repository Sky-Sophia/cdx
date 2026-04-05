package org.example.propertyms.user.service;

import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;

public interface UserService {
    User register(String username, String password);

    User authenticate(String username, String password);

    PageResult<User> listByFiltersPaged(String q, Role role, String status, int page, int pageSize);

    void updateRole(Long userId, Role role);

    void updateStatus(Long userId, String status);

    void resetPassword(Long userId, String newPassword);

    User findById(Long userId);

    User findByUsername(String username);
}

