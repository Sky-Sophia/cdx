package org.example.propertyms.user.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class UserSqlProvider {

    public String findAllWithFiltersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY created_at DESC");
        return sql.toString();
    }

    public String countWithFiltersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1");
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllWithFiltersPagedSql(Map<String, Object> params) {
        return findAllWithFiltersSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("q"))) {
            sql.append(" AND (username LIKE CONCAT('%', #{q}, '%') OR CAST(id AS CHAR) = #{q})");
        }
        if (params.get("role") != null) {
            sql.append(" AND role = #{role}");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND status = #{status}");
        }
    }
}

