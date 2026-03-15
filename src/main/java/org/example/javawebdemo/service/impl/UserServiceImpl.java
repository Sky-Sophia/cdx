package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.UserMapper;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User register(String username, String password) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        String salt = PasswordUtils.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordUtils.hash(password, salt));
        user.setRole(Role.USER);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return user;
    }

    @Override
    public User authenticate(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return null;
        }
        if (PasswordUtils.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!PasswordUtils.matches(oldPassword, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(newPassword, salt);
        userMapper.updatePassword(userId, hash, salt);
    }

    @Override
    @Transactional
    public void ensureDefaultUsers() {
        createIfMissing("admin", "admin123", Role.ADMIN);
        createIfMissing("staff", "staff123", Role.STAFF);
    }

    private void createIfMissing(String username, String password, Role role) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        String salt = PasswordUtils.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordUtils.hash(password, salt));
        user.setRole(role);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
    }

    @Override
    public List<User> listAll() {
        return userMapper.findAll();
    }

    @Override
    public List<User> listByFilters(String q, Role role, String status) {
        return userMapper.findAllWithFilters(q, role, status);
    }

    @Override
    public void updateRole(Long userId, Role role) {
        userMapper.updateRole(userId, role);
    }

    @Override
    public void updateStatus(Long userId, String status) {
        userMapper.updateStatus(userId, status);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(newPassword, salt);
        userMapper.updatePassword(userId, hash, salt);
    }

    @Override
    public User findById(Long userId) {
        return userMapper.findById(userId);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
}
