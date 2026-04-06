package org.example.propertyms.bill.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

/**
 * FeeBill 动态 SQL 提供器。
 * <p>修复原版 countSql() 不带过滤条件的 bug — 现在 countSql 与 findAllSql 共享同一组过滤逻辑。</p>
 */
public class FeeBillSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.*, u.unit_no ");
        sql.append("FROM fee_bills f ");
        sql.append("LEFT JOIN units u ON u.id = f.unit_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY f.due_date ASC, f.id DESC");
        return sql.toString();
    }

    /** 【BUG FIX】原方法签名无参数，导致带 status/billingMonth 筛选时分页总数错误。 */
    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM fee_bills f ");
        sql.append("LEFT JOIN units u ON u.id = f.unit_id ");
        sql.append("WHERE 1=1");
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
            sql.append(" AND f.billing_month = #{billingMonth}");
        }
    }
}

