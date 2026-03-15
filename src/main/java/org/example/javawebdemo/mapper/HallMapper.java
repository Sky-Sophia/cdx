package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.Hall;

@Mapper
public interface HallMapper {
    int insert(Hall hall);

    int update(Hall hall);

    int deleteById(@Param("id") Long id);

    Hall findById(@Param("id") Long id);

    List<Hall> findAll();
}
