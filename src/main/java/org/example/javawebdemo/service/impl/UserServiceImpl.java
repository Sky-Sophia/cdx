package org.example.javawebdemo.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.example.javawebdemo.mapper.UserMapper;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validatePasswordStrength(password);

        User existing = userMapper.findByUsername(normalizedUsername);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在。");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        String salt = PasswordUtils.generateSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordUtils.hash(password));
        user.setRole(Role.USER);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return user;
    }

    @Override
    public User authenticate(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername == null || normalizedUsername.isBlank() || password == null || password.isBlank()) {
            return null;
        }
        User user = userMapper.findByUsername(normalizedUsername);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return null;
        }

        if (!PasswordUtils.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            return null;
        }

        if (PasswordUtils.isLegacyHash(user.getPasswordHash())) {
            // Upgrade legacy SHA-256 hash to bcrypt after successful authentication.
            String newSalt = PasswordUtils.generateSalt();
            String newHash = PasswordUtils.hash(password);
            userMapper.updatePassword(user.getId(), newHash, newSalt);
            user.setPasswordHash(newHash);
            user.setPasswordSalt(newSalt);
        }
        return user;
    }

    @Override
    public List<User> listByFilters(String q, Role role, String status) {
        return userMapper.findAllWithFilters(q, role, status);
    }

    @Override
    public PageResult<User> listByFiltersPaged(String q, Role role, String status, int page, int pageSize) {
        long total = userMapper.countWithFilters(q, role, status);
        int offset = PageResult.calcOffset(page, pageSize);
        List<User> items = userMapper.findAllWithFiltersPaged(q, role, status, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    @Transactional
    public void updateRole(Long userId, Role role) {
        userMapper.updateRole(userId, role);
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, String status) {
        userMapper.updateStatus(userId, status);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在。");
        }
        validatePasswordStrength(newPassword);
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(newPassword);
        userMapper.updatePassword(userId, hash, salt);
    }

    @Override
    public User findById(Long userId) {
        return userMapper.findById(userId);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userMapper.findByUsername(normalizeUsername(username));
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("密码至少 8 位，且必须包含字母、数字和特殊字符。");
        }
    }

    private void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名仅支持 3-20 位字母、数字或下划线。");
        }
    }
}
