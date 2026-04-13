package org.example.propertyms.workorder.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class WorkOrderSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append(WorkOrderMapper.BASE_SELECT);
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY w.created_at DESC, w.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM work_orders w ");
        sql.append("LEFT JOIN units u ON u.id = w.unit_id ");
        sql.append("LEFT JOIN residents r ON r.id = w.resident_id ");
        sql.append("LEFT JOIN persons p ON p.id = r.person_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllPagedSql(Map<String, Object> params) {
        return findAllSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("keyword"))) {
            sql.append(" AND (w.order_no LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.unit_no LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR COALESCE(p.full_name, r.name) LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR COALESCE(p.phone, r.phone) LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND w.status = #{status}");
        }
        if (SqlProviderHelper.isNotBlank(params.get("priority"))) {
            sql.append(" AND w.priority = #{priority}");
        }
    }
}


