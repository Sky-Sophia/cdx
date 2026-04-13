package org.example.propertyms.user.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class UserSqlProvider {

    public String findAllWithFiltersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder(UserMapper.BASE_SELECT);
        sql.append(" WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY ua.created_at DESC, ua.id DESC");
        return sql.toString();
    }

    public String countWithFiltersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM user_accounts ua WHERE 1=1");
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllWithFiltersPagedSql(Map<String, Object> params) {
        return findAllWithFiltersSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("q"))) {
            sql.append(" AND (ua.username LIKE CONCAT('%', #{q}, '%') OR CAST(ua.id AS CHAR) = #{q})");
        }
        if (params.get("role") != null) {
            sql.append(" AND ua.account_role = #{role}");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND ua.status = #{status}");
        }
    }
}


