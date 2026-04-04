package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;

public interface UserService {
    User register(String username, String password);

    User authenticate(String username, String password);

    List<User> listByFilters(String q, Role role, String status);

    PageResult<User> listByFiltersPaged(String q, Role role, String status, int page, int pageSize);

    void updateRole(Long userId, Role role);

    void updateStatus(Long userId, String status);

    void resetPassword(Long userId, String newPassword);

    User findById(Long userId);

    User findByUsername(String username);

}
