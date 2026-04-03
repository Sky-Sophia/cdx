package org.example.javawebdemo.mapper.provider;

import java.util.Map;

public class ResidentSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.*, u.unit_no ");
        sql.append("FROM residents r ");
        sql.append("LEFT JOIN units u ON u.id = r.unit_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("keyword"))) {
            sql.append(" AND (r.name LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR r.phone LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.unit_no LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (isNotBlank(params.get("status"))) {
            sql.append(" AND r.status = #{status}");
        }

        sql.append(" ORDER BY r.updated_at DESC, r.id DESC");
        return sql.toString();
    }

    private boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}
