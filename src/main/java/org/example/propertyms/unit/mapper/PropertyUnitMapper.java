package org.example.propertyms.unit.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.unit.model.PropertyUnit;

@Mapper
public interface PropertyUnitMapper {
    String BASE_COLUMNS = """
            u.id,
            u.building_id,
            b.name AS building_name,
            u.unit_no,
            u.floor_no,
            u.area_m2,
            u.occupancy_status,
            u.owner_resident_id,
            r.name AS owner_name,
            r.phone AS owner_phone,
            u.created_at,
            u.updated_at
            """;

    @SelectProvider(type = PropertyUnitSqlProvider.class, method = "countSql")
    long count(@Param("keyword") String keyword,
               @Param("buildingId") Long buildingId,
               @Param("status") String status);

    @SelectProvider(type = PropertyUnitSqlProvider.class, method = "findAllPagedSql")
    List<PropertyUnit> findAllPaged(@Param("keyword") String keyword,
                                    @Param("buildingId") Long buildingId,
                                    @Param("status") String status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @SelectProvider(type = PropertyUnitSqlProvider.class, method = "findAllSql")
    List<PropertyUnit> findAll(@Param("keyword") String keyword,
                               @Param("buildingId") Long buildingId,
                               @Param("status") String status);

    @Select("""
            SELECT
            """ + BASE_COLUMNS + """
            FROM units u
            LEFT JOIN buildings b ON b.id = u.building_id
            LEFT JOIN residents r ON r.id = u.owner_resident_id
            WHERE u.id = #{id}
            """)
    PropertyUnit findById(@Param("id") Long id);

    @Select("SELECT id, unit_no FROM units ORDER BY unit_no")
    List<PropertyUnit> findAllSimple();

    @Insert("""
            INSERT INTO units (building_id, unit_no, floor_no, area_m2, occupancy_status)
            VALUES (#{buildingId}, #{unitNo}, #{floorNo}, #{areaM2}, #{occupancyStatus})
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
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void update(PropertyUnit unit);

    @Delete("DELETE FROM units WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM units")
    long countAll();
}
