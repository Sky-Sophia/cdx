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

    @Select("SELECT * FROM users WHERE id = #{id} AND status = 'ACTIVE' LIMIT 1")
    User findActiveUserById(@Param("id") Long id);

    @Select("SELECT * FROM users WHERE username = #{username} AND status = 'ACTIVE' LIMIT 1")
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
