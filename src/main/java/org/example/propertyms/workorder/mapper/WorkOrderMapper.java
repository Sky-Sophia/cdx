package org.example.propertyms.workorder.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.workorder.model.WorkOrder;

@Mapper
public interface WorkOrderMapper {
    String BASE_SELECT = """
            SELECT w.id,
                   w.order_no,
                   w.unit_id,
                   w.resident_id,
                   w.assignee_employee_id,
                   u.unit_no,
                   p.full_name AS resident_name,
                   p.phone AS phone,
                   w.category,
                   w.priority,
                   w.description,
                   w.status,
                   ap.full_name AS assignee,
                   w.scheduled_at,
                   w.finished_at,
                   w.created_at,
                   w.updated_at
            FROM work_orders w
            LEFT JOIN units u ON u.id = w.unit_id
            LEFT JOIN residents r ON r.id = w.resident_id
            LEFT JOIN persons p ON p.id = r.person_id
            LEFT JOIN employees ae ON ae.id = w.assignee_employee_id
            LEFT JOIN persons ap ON ap.id = ae.person_id
            """;

    @SelectProvider(type = WorkOrderSqlProvider.class, method = "countSql")
    long count(@Param("keyword") String keyword,
               @Param("status") String status,
               @Param("priority") String priority);

    @SelectProvider(type = WorkOrderSqlProvider.class, method = "findAllPagedSql")
    List<WorkOrder> findAllPaged(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("priority") String priority,
                                 @Param("offset") int offset,
                                 @Param("pageSize") int pageSize);

    @SelectProvider(type = WorkOrderSqlProvider.class, method = "findAllSql")
    List<WorkOrder> findAll(@Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("priority") String priority);

    @Select(BASE_SELECT + " WHERE w.id = #{id}")
    WorkOrder findById(@Param("id") Long id);

    @Select(BASE_SELECT + " ORDER BY w.created_at DESC LIMIT #{limit}")
    List<WorkOrder> findRecent(@Param("limit") int limit);

    @Insert("""
            INSERT INTO work_orders (order_no, unit_id, resident_id, category, priority, description, status, assignee_employee_id, scheduled_at)
            VALUES (#{orderNo}, #{unitId}, #{residentId}, #{category}, #{priority}, #{description}, #{status}, #{assigneeEmployeeId}, #{scheduledAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkOrder workOrder);

    @Select("""
            SELECT r.id
            FROM residents r
            WHERE r.unit_id = #{unitId}
              AND r.status = 'ACTIVE'
            ORDER BY CASE r.resident_type WHEN 'OWNER' THEN 0 ELSE 1 END, r.id
            LIMIT 1
            """)
    Long findActiveResidentIdByUnitId(@Param("unitId") Long unitId);

    @Update("""
            UPDATE work_orders
            SET status = #{status},
                assignee_employee_id = #{assigneeEmployeeId},
                scheduled_at = #{scheduledAt},
                finished_at = #{finishedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("assigneeEmployeeId") Long assigneeEmployeeId,
                     @Param("scheduledAt") LocalDateTime scheduledAt,
                     @Param("finishedAt") LocalDateTime finishedAt);

    @Select("SELECT COUNT(*) FROM work_orders WHERE status IN ('OPEN', 'IN_PROGRESS')")
    long countOpen();
}


