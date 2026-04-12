package org.example.propertyms.bill.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class FeeBillSqlProvider {
    private static final String SELECT_FIELDS = """
            SELECT f.id,
                   f.bill_no,
                   f.unit_id,
                   u.unit_no,
                   DATE_FORMAT(f.billing_month, '%Y-%m') AS billing_month,
                   f.amount,
                   f.paid_amount,
                   f.status,
                   f.due_date,
                   f.paid_at,
                   f.remarks,
                   f.created_at,
                   f.updated_at
            FROM fee_bills f
            LEFT JOIN units u ON u.id = f.unit_id
            WHERE 1=1
            """;

    private static final String COUNT_FIELDS = """
            SELECT COUNT(*)
            FROM fee_bills f
            LEFT JOIN units u ON u.id = f.unit_id
            WHERE 1=1
            """;

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder(SELECT_FIELDS);
        appendFilters(sql, params);
        sql.append(" ORDER BY f.due_date ASC, f.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder(COUNT_FIELDS);
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllPagedSql(Map<String, Object> params) {
        return findAllSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("keyword"))) {
            sql.append(" AND (f.bill_no LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.unit_no LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND f.status = #{status}");
        }
        if (SqlProviderHelper.isNotBlank(params.get("billingMonth"))) {
            sql.append(" AND f.billing_month = STR_TO_DATE(CONCAT(#{billingMonth}, '-01'), '%Y-%m-%d')");
        }
    }
}
