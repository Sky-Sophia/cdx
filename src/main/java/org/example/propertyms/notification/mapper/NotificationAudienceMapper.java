package org.example.propertyms.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;

@Mapper
public interface NotificationAudienceMapper {
    String ACTIVE_USER_SELECT = """
            SELECT ua.id,
                   ua.username,
                   ua.password_hash AS password,
                   ua.account_role AS role,
                   ua.status,
                   resident_link.unit_id AS unit_id,
                   employee_link.department_code AS department_code,
                   ua.created_at,
                   ua.updated_at
            FROM user_accounts ua
            LEFT JOIN (
                SELECT r.account_id, MAX(r.unit_id) AS unit_id
                FROM residents r
                WHERE r.account_id IS NOT NULL
                GROUP BY r.account_id
            ) resident_link ON resident_link.account_id = ua.id
            LEFT JOIN employees employee_link ON employee_link.account_id = ua.id
            """;

    @Select(ACTIVE_USER_SELECT + " WHERE ua.id = #{id} AND ua.status = 'ACTIVE' LIMIT 1")
    User findActiveUserById(@Param("id") Long id);

    @Select(ACTIVE_USER_SELECT + " WHERE ua.username = #{username} AND ua.status = 'ACTIVE' LIMIT 1")
    User findActiveUserByUsername(@Param("username") String username);

    @SelectProvider(type = NotificationAudienceSqlProvider.class, method = "findAllActiveUsersSql")
    List<User> findAllActiveUsers(@Param("excludeUserId") Long excludeUserId);

    @SelectProvider(type = NotificationAudienceSqlProvider.class, method = "findActiveUsersByDepartmentSql")
    List<User> findActiveUsersByDepartment(@Param("departmentCode") String departmentCode,
                                           @Param("fallbackRole") Role fallbackRole,
                                           @Param("excludeUserId") Long excludeUserId);

    @SelectProvider(type = NotificationAudienceSqlProvider.class, method = "findActiveUsersByBuildingSql")
    List<User> findActiveUsersByBuilding(@Param("targetValue") String targetValue,
                                         @Param("excludeUserId") Long excludeUserId);

    @SelectProvider(type = NotificationAudienceSqlProvider.class, method = "findActiveUsersByDueBillSql")
    List<User> findActiveUsersByDueBill(@Param("excludeUserId") Long excludeUserId);

    @SelectProvider(type = NotificationAudienceSqlProvider.class, method = "findActiveUsersByCompletedWorkOrderSql")
    List<User> findActiveUsersByCompletedWorkOrder(@Param("excludeUserId") Long excludeUserId);
}

