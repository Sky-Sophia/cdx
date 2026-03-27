package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.javawebdemo.model.Resident;

@Mapper
public interface ResidentMapper {

    @Select({
            "<script>",
            "SELECT r.*, u.unit_no",
            "FROM residents r",
            "LEFT JOIN units u ON u.id = r.unit_id",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (r.name LIKE CONCAT('%', #{keyword}, '%')",
            "      OR r.phone LIKE CONCAT('%', #{keyword}, '%')",
            "      OR u.unit_no LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "  <if test='status != null and status != \"\"'>",
            "    AND r.status = #{status}",
            "  </if>",
            "</where>",
            "ORDER BY r.updated_at DESC, r.id DESC",
            "</script>"
    })
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
    int update(Resident resident);

    @Delete("DELETE FROM residents WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM residents WHERE status = 'ACTIVE'")
    long countActive();
}
