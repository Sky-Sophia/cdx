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
    String BASE_SELECT = """
            SELECT r.id,
                   r.person_id,
                   r.account_id,
                   r.unit_id,
                   u.unit_no,
                   p.full_name AS name,
                   p.phone,
                   p.identity_no,
                   r.resident_type,
                   r.status,
                   r.move_in_date,
                   r.move_out_date,
                   r.created_at,
                   r.updated_at
            FROM residents r
            LEFT JOIN units u ON u.id = r.unit_id
            LEFT JOIN persons p ON p.id = r.person_id
            """;

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

    @Select(BASE_SELECT + " WHERE r.id = #{id}")
    Resident findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO residents (person_id, account_id, unit_id, resident_type, status, move_in_date, move_out_date)
            VALUES (#{personId}, #{accountId}, #{unitId}, #{residentType}, #{status}, #{moveInDate}, #{moveOutDate})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resident resident);

    @Update("""
            UPDATE residents
            SET person_id = #{personId},
                account_id = #{accountId},
                unit_id = #{unitId},
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
              AND resident_type = 'OWNER'
              AND status = 'ACTIVE'
              AND id <> #{residentId}
            """)
    void moveOutOtherActiveOwners(@Param("unitId") Long unitId, @Param("residentId") Long residentId);

    @Update("""
            UPDATE units u
            LEFT JOIN (
                SELECT r.unit_id,
                       MAX(CASE WHEN r.resident_type = 'OWNER' AND r.status = 'ACTIVE' THEN r.id END) AS owner_resident_id,
                       MAX(IF(r.resident_type = 'TENANT' AND r.status = 'ACTIVE', 1, 0)) AS has_active_tenant,
                       MAX(IF(r.resident_type = 'OWNER' AND r.status = 'ACTIVE', 1, 0)) AS has_active_owner,
                       MAX(IF(r.resident_type = 'OWNER', 1, 0)) AS has_owner_history
                FROM residents r
                WHERE r.unit_id = #{unitId}
                GROUP BY r.unit_id
            ) resident_stats ON resident_stats.unit_id = u.id
            SET u.owner_resident_id = resident_stats.owner_resident_id,
                u.occupancy_status = CASE
                    WHEN COALESCE(resident_stats.has_active_tenant, 0) = 1 THEN 'RENTED'
                    WHEN COALESCE(resident_stats.has_active_owner, 0) = 1 THEN 'SELF_OCCUPIED'
                    WHEN COALESCE(resident_stats.has_owner_history, 0) = 1 THEN 'VACANT'
                    ELSE 'UNSOLD'
                END,
                u.updated_at = CURRENT_TIMESTAMP
            WHERE u.id = #{unitId}
            """)
    void refreshUnitOccupancy(@Param("unitId") Long unitId);

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

