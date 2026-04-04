package org.example.javawebdemo.mapper.provider;

import java.util.Map;

public class WorkOrderSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT w.*, u.unit_no ");
        sql.append("FROM work_orders w ");
        sql.append("LEFT JOIN units u ON u.id = w.unit_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("status"))) {
            sql.append(" AND w.status = #{status}");
        }
        if (isNotBlank(params.get("priority"))) {
            sql.append(" AND w.priority = #{priority}");
        }

        sql.append(" ORDER BY w.created_at DESC, w.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM work_orders w ");
        sql.append("LEFT JOIN units u ON u.id = w.unit_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("status"))) {
            sql.append(" AND w.status = #{status}");
        }
        if (isNotBlank(params.get("priority"))) {
            sql.append(" AND w.priority = #{priority}");
        }

        return sql.toString();
    }

    public String findAllPagedSql(Map<String, Object> params) {
        return findAllSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}
