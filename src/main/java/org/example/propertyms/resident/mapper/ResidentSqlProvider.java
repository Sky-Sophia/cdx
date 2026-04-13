package org.example.propertyms.resident.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class ResidentSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.*, u.unit_no ");
        sql.append("FROM residents r ");
        sql.append("LEFT JOIN units u ON u.id = r.unit_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY r.updated_at DESC, r.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM residents r ");
        sql.append("LEFT JOIN units u ON u.id = r.unit_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllPagedSql(Map<String, Object> params) {
        return findAllSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("keyword"))) {
            sql.append(" AND (r.name LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR r.phone LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.unit_no LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND r.status = #{status}");
        }
    }
}


