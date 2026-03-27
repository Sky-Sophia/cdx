package org.example.javawebdemo.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.javawebdemo.model.WorkOrder;

@Mapper
public interface WorkOrderMapper {

    @Select({
            "<script>",
            "SELECT w.*, u.unit_no",
            "FROM work_orders w",
            "LEFT JOIN units u ON u.id = w.unit_id",
            "<where>",
            "  <if test='status != null and status != \"\"'>",
            "    AND w.status = #{status}",
            "  </if>",
            "  <if test='priority != null and priority != \"\"'>",
            "    AND w.priority = #{priority}",
            "  </if>",
            "</where>",
            "ORDER BY w.created_at DESC, w.id DESC",
            "</script>"
    })
    List<WorkOrder> findAll(@Param("status") String status,
                            @Param("priority") String priority);

    @Select("SELECT w.*, u.unit_no FROM work_orders w LEFT JOIN units u ON u.id = w.unit_id WHERE w.id = #{id}")
    WorkOrder findById(@Param("id") Long id);

    @Select("SELECT w.*, u.unit_no FROM work_orders w LEFT JOIN units u ON u.id = w.unit_id ORDER BY w.created_at DESC LIMIT #{limit}")
    List<WorkOrder> findRecent(@Param("limit") int limit);

    @Insert("""
            INSERT INTO work_orders (order_no, unit_id, resident_name, phone, category, priority, description, status, assignee, scheduled_at)
            VALUES (#{orderNo}, #{unitId}, #{residentName}, #{phone}, #{category}, #{priority}, #{description}, #{status}, #{assignee}, #{scheduledAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkOrder workOrder);

    @Update("""
            UPDATE work_orders
            SET status = #{status},
                assignee = #{assignee},
                scheduled_at = #{scheduledAt},
                finished_at = #{finishedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("assignee") String assignee,
                     @Param("scheduledAt") LocalDateTime scheduledAt,
                     @Param("finishedAt") LocalDateTime finishedAt);

    @Select("SELECT COUNT(*) FROM work_orders WHERE status IN ('OPEN', 'IN_PROGRESS')")
    long countOpen();
}
