package org.example.propertyms.resident.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.resident.model.Resident;

@Mapper
public interface ResidentMapper {

    @SelectProvider(type = ResidentSqlProvider.class, method = "countSql")
    long count(@Param("keyword") String keyword,
               @Param("status") String status);

    @SelectProvider(type = ResidentSqlProvider.class, method = "findAllPagedSql")
    List<Resident> findAllPaged(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @SelectProvider(type = ResidentSqlProvider.class, method = "findAllSql")
    List<Resident> findAll(@Param("keyword") String keyword,
                           @Param("status") String status);

    @Select("SELECT r.*, u.unit_no FROM residents r LEFT JOIN units u ON u.id = r.unit_id WHERE r.id = #{id}")
    Resident findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO residents (unit_id, name, phone, identity_no, resident_type, status, move_in_date, move_out_date)
            VALUES (#{unitId}, #{name}, #{phone}, #{identityNo}, #{residentType}, #{status}, #{moveInDate}, #{moveOutDate})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resident resident);

    @Update("""
            UPDATE residents
            SET unit_id = #{unitId},
                name = #{name},
                phone = #{phone},
                identity_no = #{identityNo},
                resident_type = #{residentType},
                status = #{status},
                move_in_date = #{moveInDate},
                move_out_date = #{moveOutDate},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void update(Resident resident);

    @Delete("DELETE FROM residents WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Update("""
            UPDATE residents
            SET status = 'MOVED_OUT',
                move_out_date = COALESCE(move_out_date, CURRENT_DATE),
                updated_at = CURRENT_TIMESTAMP
            WHERE unit_id = #{unitId}
              AND status = 'ACTIVE'
              AND id <> #{residentId}
            """)
    int moveOutOtherActiveOwners(@Param("unitId") Long unitId, @Param("residentId") Long residentId);

    @Update("""
            UPDATE units u
            JOIN residents r ON r.id = #{residentId}
            SET u.owner_resident_id = r.id,
                u.occupancy_status = 'OCCUPIED',
                u.updated_at = CURRENT_TIMESTAMP
            WHERE u.id = r.unit_id
            """)
    int syncUnitOwnerFromResident(@Param("residentId") Long residentId);

    @Update("""
            UPDATE units u
            SET u.owner_resident_id = NULL,
                u.occupancy_status = 'VACANT',
                u.updated_at = CURRENT_TIMESTAMP
            WHERE u.owner_resident_id = #{residentId}
            """)
    int clearUnitOwnerIfResidentInactive(@Param("residentId") Long residentId);

    @Select("""
            SELECT COUNT(*)
            FROM residents
            WHERE status = 'ACTIVE'
              AND (move_out_date IS NULL OR move_out_date >= CURRENT_DATE)
            """)
    long countActive();

    @Select("""
            SELECT COUNT(DISTINCT unit_id)
            FROM residents
            WHERE status = 'ACTIVE'
              AND (move_out_date IS NULL OR move_out_date >= CURRENT_DATE)
            """)
    long countOccupiedUnits();
}
