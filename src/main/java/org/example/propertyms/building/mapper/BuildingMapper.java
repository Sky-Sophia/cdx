package org.example.propertyms.building.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.propertyms.building.model.Building;

@Mapper
public interface BuildingMapper {

    @Select("SELECT * FROM buildings ORDER BY code")
    List<Building> findAll();

    @Select("SELECT COUNT(*) FROM buildings")
    long countAll();
}

