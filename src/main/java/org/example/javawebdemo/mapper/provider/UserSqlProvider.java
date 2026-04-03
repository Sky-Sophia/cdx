package org.example.javawebdemo.mapper.provider;

import java.util.Map;

public class UserSqlProvider {

    public String findAllWithFiltersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        if (isNotBlank(params.get("q"))) {
            sql.append(" AND username LIKE CONCAT('%', #{q}, '%')");
        }
        if (params.get("role") != null) {
            sql.append(" AND role = #{role}");
        }
        if (isNotBlank(params.get("status"))) {
            sql.append(" AND status = #{status}");
        }
        sql.append(" ORDER BY created_at DESC");
        return sql.toString();
    }

    private boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}
