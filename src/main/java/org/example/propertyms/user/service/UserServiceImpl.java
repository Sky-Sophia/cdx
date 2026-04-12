package org.example.propertyms.user.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.PasswordUtils;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.example.propertyms.user.mapper.UserMapper;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.model.UserStatus;
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
        user.setPassword(PasswordUtils.hash(password));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE.name());
        user.setDepartmentCode(NotificationDepartment.defaultForRole(Role.USER).getCode());
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
        if (user == null || !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            return null;
        }
        if (!PasswordUtils.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    @Override
    public PageResult<User> listByFiltersPaged(String q, Role role, String status, int page, int pageSize) {
        long total = userMapper.countWithFilters(q, role, status);
        int offset = PageResult.calcOffset(page, pageSize);
        List<User> items = userMapper.findAllWithFiltersPaged(q, role, status, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    public List<User> listByFilters(String q, Role role, String status) {
        return userMapper.findAllWithFilters(q, role, status);
    }

    @Override
    @Transactional
    public void updateRole(Long userId, Role role) {
        userMapper.updateRole(userId, role);
    }

    @Override
    @Transactional
    public void updateDepartmentCode(Long userId, String departmentCode) {
        userMapper.updateDepartmentCode(userId, departmentCode);
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
        String password = PasswordUtils.hash(newPassword);
        userMapper.updatePassword(userId, password);
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

