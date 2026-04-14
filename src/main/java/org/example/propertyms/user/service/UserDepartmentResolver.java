package org.example.propertyms.user.service;

import java.util.Locale;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.example.propertyms.user.model.Role;
import org.springframework.stereotype.Component;

/**
 * 用户所属部门解析器，统一处理角色默认部门与自定义部门校验逻辑。
 */
@Component
public class UserDepartmentResolver {
    private final DepartmentService departmentService;

    public UserDepartmentResolver(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    public String defaultDepartmentCode(Role role) {
        NotificationDepartment department = NotificationDepartment.defaultForRole(role);
        return department == null ? null : department.getCode();
    }

    public String resolve(Role role, String requestedDepartmentCode) {
        if (role == null || role == Role.RESIDENT) {
            return null;
        }
        if (requestedDepartmentCode == null || requestedDepartmentCode.isBlank()) {
            return defaultDepartmentCode(role);
        }
        String normalizedCode = requestedDepartmentCode.trim().toUpperCase(Locale.ROOT);
        if (!departmentService.isEnabledCode(normalizedCode)) {
            throw new IllegalArgumentException("所选部门不存在或已停用。");
        }
        return normalizedCode;
    }
}
