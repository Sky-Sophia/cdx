package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.User;

@Mapper
public interface UserMapper {
    int insert(User user);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    List<User> findAll();

    List<User> findAllWithFilters(@Param("q") String q,
                                  @Param("role") Role role,
                                  @Param("status") String status);

    int updateRole(@Param("id") Long id, @Param("role") Role role);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updatePassword(@Param("id") Long id, @Param("hash") String hash, @Param("salt") String salt);
}
