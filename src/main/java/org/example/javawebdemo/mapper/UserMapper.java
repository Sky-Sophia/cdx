package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;

@Mapper
public interface UserMapper {
    @Insert("""
            INSERT INTO users (username, password_hash, password_salt, role, status)
            VALUES (#{username}, #{passwordHash}, #{passwordSalt}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> findAll();

    @Select({
            "<script>",
            "SELECT * FROM users",
            "<where>",
            "  <if test='q != null and q != \"\"'>",
            "    AND username LIKE CONCAT('%', #{q}, '%')",
            "  </if>",
            "  <if test='role != null'>",
            "    AND role = #{role}",
            "  </if>",
            "  <if test='status != null and status != \"\"'>",
            "    AND status = #{status}",
            "  </if>",
            "</where>",
            "ORDER BY created_at DESC",
            "</script>"
    })
    List<User> findAllWithFilters(@Param("q") String q,
                                  @Param("role") Role role,
                                  @Param("status") String status);

    @Update("UPDATE users SET role = #{role}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") Role role);

    @Update("UPDATE users SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE users SET password_hash = #{hash}, password_salt = #{salt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash, @Param("salt") String salt);
}
