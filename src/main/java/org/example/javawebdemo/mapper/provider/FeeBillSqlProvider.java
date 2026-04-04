package org.example.javawebdemo.mapper.provider;

import java.util.Map;

public class FeeBillSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.*, u.unit_no ");
        sql.append("FROM fee_bills f ");
        sql.append("LEFT JOIN units u ON u.id = f.unit_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("status"))) {
            sql.append(" AND f.status = #{status}");
        }
        if (isNotBlank(params.get("billingMonth"))) {
            sql.append(" AND f.billing_month = #{billingMonth}");
        }

        sql.append(" ORDER BY f.due_date ASC, f.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM fee_bills f ");
        sql.append("LEFT JOIN units u ON u.id = f.unit_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("status"))) {
            sql.append(" AND f.status = #{status}");
        }
        if (isNotBlank(params.get("billingMonth"))) {
            sql.append(" AND f.billing_month = #{billingMonth}");
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
