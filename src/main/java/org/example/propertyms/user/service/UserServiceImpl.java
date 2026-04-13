package org.example.propertyms.user.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.PasswordUtils;
import org.example.propertyms.user.mapper.UserMapper;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;
import org.example.propertyms.user.model.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    public UserServiceImpl(UserMapper userMapper, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
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
        user.setRole(Role.RESIDENT);
        user.setStatus(UserStatus.ACTIVE.name());
        user.setDepartmentCode(null);
        Long personId = insertPerson(normalizedUsername);
        Long accountId = insertUserAccount(personId, user);
        return userMapper.findById(accountId);
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
        User existing = userMapper.findById(userId);
        if (existing == null || role == null) {
            return;
        }

        jdbcTemplate.update("""
                UPDATE user_accounts
                SET account_role = ?,
                    account_type = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, role.name(), accountTypeForRole(role), userId);

        if (role == Role.RESIDENT) {
            jdbcTemplate.update("DELETE FROM employees WHERE account_id = ?", userId);
            bindResidentAccount(userId);
            return;
        }

        jdbcTemplate.update("UPDATE residents SET account_id = NULL WHERE account_id = ?", userId);
        ensureEmployeeRecord(userId, existing.getUsername(), role, resolveDepartmentCode(role), existing.getStatus());
    }

    @Override
    @Transactional
    public void updateDepartmentCode(Long userId, String departmentCode) {
        User existing = userMapper.findById(userId);
        if (existing == null || existing.getRole() == null || existing.getRole() == Role.RESIDENT) {
            return;
        }
        ensureEmployeeRecord(userId,
                existing.getUsername(),
                existing.getRole(),
                resolveDepartmentCode(existing.getRole(), departmentCode),
                existing.getStatus());
    }

    @Override
    @Transactional
    public void updateStatus(Long userId, String status) {
        User existing = userMapper.findById(userId);
        if (existing == null || status == null || status.isBlank()) {
            return;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        jdbcTemplate.update("""
                UPDATE user_accounts
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, normalizedStatus, userId);
        jdbcTemplate.update("""
                UPDATE employees
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE account_id = ?
                """, normalizedStatus, userId);
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
        jdbcTemplate.update("""
                UPDATE user_accounts
                SET password_hash = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, password, userId);
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

    private Long insertPerson(String fullName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO persons (full_name, gender, created_at, updated_at)
                    VALUES (?, 'UNKNOWN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            statement.setString(1, fullName);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建人员档案失败。");
        }
        return key.longValue();
    }

    private Long insertUserAccount(Long personId, User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO user_accounts (
                        person_id, username, password_hash, account_type, account_role, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            statement.setLong(1, personId);
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, accountTypeForRole(user.getRole()));
            statement.setString(5, user.getRole().name());
            statement.setString(6, user.getStatus());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建登录账号失败。");
        }
        return key.longValue();
    }

    private void bindResidentAccount(Long accountId) {
        Long personId = jdbcTemplate.query("""
                        SELECT person_id
                        FROM user_accounts
                        WHERE id = ?
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong(1) : null,
                accountId);
        if (personId == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE residents
                SET account_id = NULL
                WHERE account_id = ?
                """, accountId);
        Long residentId = jdbcTemplate.query("""
                        SELECT id
                        FROM residents
                        WHERE person_id = ?
                          AND (account_id IS NULL OR account_id = ?)
                        ORDER BY CASE resident_type WHEN 'OWNER' THEN 0 ELSE 1 END, id
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong(1) : null,
                personId, accountId);
        if (residentId != null) {
            jdbcTemplate.update("""
                    UPDATE residents
                    SET account_id = ?
                    WHERE id = ?
                    """, accountId, residentId);
        }
    }

    private void ensureEmployeeRecord(Long accountId,
                                      String username,
                                      Role role,
                                      String departmentCode,
                                      String status) {
        Long personId = jdbcTemplate.query("""
                        SELECT person_id
                        FROM user_accounts
                        WHERE id = ?
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong(1) : null,
                accountId);
        if (personId == null) {
            throw new IllegalStateException("账号关联人员信息不存在。");
        }

        String employeeNo = username == null || username.isBlank() ? "EMP-" + accountId : username.trim();
        String normalizedDepartmentCode = resolveDepartmentCode(role, departmentCode);
        String normalizedStatus = (status == null || status.isBlank())
                ? UserStatus.ACTIVE.name()
                : status.trim().toUpperCase(Locale.ROOT);

        jdbcTemplate.update("""
                INSERT INTO employees (
                    person_id, account_id, employee_no, department_code, employee_role, status, hire_date, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    employee_no = VALUES(employee_no),
                    department_code = VALUES(department_code),
                    employee_role = VALUES(employee_role),
                    status = VALUES(status),
                    updated_at = CURRENT_TIMESTAMP
                """, personId, accountId, employeeNo, normalizedDepartmentCode, role.name(), normalizedStatus);
    }

    private String accountTypeForRole(Role role) {
        return role == Role.RESIDENT ? "RESIDENT" : "EMPLOYEE";
    }

    private String resolveDepartmentCode(Role role) {
        return resolveDepartmentCode(role, null);
    }

    private String resolveDepartmentCode(Role role, String departmentCode) {
        if (role == null || role == Role.RESIDENT) {
            return null;
        }
        if (departmentCode != null && !departmentCode.isBlank()) {
            return departmentCode.trim().toUpperCase(Locale.ROOT);
        }
        return switch (role) {
            case SUPER_ADMIN -> "OFFICE";
            case ADMIN -> "MANAGEMENT";
            case ACCOUNTANT -> "FINANCE";
            case ENGINEER -> "ENGINEERING";
            case RESIDENT -> null;
        };
    }
}


