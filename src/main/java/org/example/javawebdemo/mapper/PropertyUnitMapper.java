package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.javawebdemo.mapper.provider.PropertyUnitSqlProvider;
import org.example.javawebdemo.model.PropertyUnit;

@Mapper
public interface PropertyUnitMapper {

    @SelectProvider(type = PropertyUnitSqlProvider.class, method = "findAllSql")
    List<PropertyUnit> findAll(@Param("keyword") String keyword,
                               @Param("buildingId") Long buildingId,
                               @Param("status") String status);

    @Select("SELECT u.*, b.name AS building_name FROM units u LEFT JOIN buildings b ON b.id = u.building_id WHERE u.id = #{id}")
    PropertyUnit findById(@Param("id") Long id);

    @Select("SELECT id, unit_no FROM units ORDER BY unit_no ASC")
    List<PropertyUnit> findAllSimple();

    @Insert("""
            INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status, owner_name, owner_phone)
            VALUES (#{buildingId}, #{unitNo}, #{floorNo}, #{areaM2}, #{occupancyStatus}, #{ownerName}, #{ownerPhone})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PropertyUnit unit);

    @Update("""
            UPDATE units
            SET building_id = #{buildingId},
                unit_no = #{unitNo},
                floor_no = #{floorNo},
                area_m2 = #{areaM2},
                occupancy_status = #{occupancyStatus},
                owner_name = #{ownerName},
                owner_phone = #{ownerPhone},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void update(PropertyUnit unit);

    @Delete("DELETE FROM units WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM units")
    long countAll();
}
