package org.example.propertyms.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;

@Mapper
public interface UserMapper {
    String BASE_SELECT = """
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

    @Select(BASE_SELECT + " WHERE ua.username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);

    @Select(BASE_SELECT + " WHERE ua.id = #{id} LIMIT 1")
    User findById(@Param("id") Long id);

    @SelectProvider(type = UserSqlProvider.class, method = "countWithFiltersSql")
    long countWithFilters(@Param("q") String q,
                          @Param("role") Role role,
                          @Param("status") String status);

    @SelectProvider(type = UserSqlProvider.class, method = "findAllWithFiltersSql")
    List<User> findAllWithFilters(@Param("q") String q,
                                  @Param("role") Role role,
                                  @Param("status") String status);

    @SelectProvider(type = UserSqlProvider.class, method = "findAllWithFiltersPagedSql")
    List<User> findAllWithFiltersPaged(@Param("q") String q,
                                       @Param("role") Role role,
                                       @Param("status") String status,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    @Update("""
            UPDATE user_accounts
            SET account_role = #{role},
                account_type = CASE
                    WHEN #{role} = 'RESIDENT' THEN 'RESIDENT'
                    ELSE 'EMPLOYEE'
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updateRole(@Param("id") Long id, @Param("role") Role role);

    @Update("""
            UPDATE employees
            SET department_code = #{departmentCode},
                updated_at = CURRENT_TIMESTAMP
            WHERE account_id = #{id}
            """)
    void updateDepartmentCode(@Param("id") Long id, @Param("departmentCode") String departmentCode);

    @Update("""
            UPDATE user_accounts
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("""
            UPDATE user_accounts
            SET password_hash = #{password},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updatePassword(@Param("id") Long id, @Param("password") String password);
}


