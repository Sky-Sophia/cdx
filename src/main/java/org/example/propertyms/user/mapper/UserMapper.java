package org.example.propertyms.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.user.model.Role;
import org.example.propertyms.user.model.User;

@Mapper
public interface UserMapper {
    @Insert("""
            INSERT INTO users (username, password, role, status)
            VALUES (#{username}, #{password}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE id = #{id}")
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

    @Update("UPDATE users SET role = #{role}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateRole(@Param("id") Long id, @Param("role") Role role);

    @Update("UPDATE users SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE users SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updatePassword(@Param("id") Long id, @Param("password") String password);
}

