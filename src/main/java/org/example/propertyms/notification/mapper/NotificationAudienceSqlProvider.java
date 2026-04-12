package org.example.propertyms.notification.mapper;

import java.util.Map;

public class NotificationAudienceSqlProvider {

    public String findAllActiveUsersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder("SELECT u.* FROM users u WHERE u.status = 'ACTIVE'");
        appendExclude(sql, params);
        sql.append(" ORDER BY u.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByDepartmentSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT u.* FROM users u WHERE u.status = 'ACTIVE'");
        sql.append(" AND (u.department_code = #{departmentCode}");
        sql.append(" OR ((u.department_code IS NULL OR u.department_code = '') AND u.role = #{fallbackRole})");
        sql.append(" OR (#{departmentCode} = 'NONE' AND (u.department_code IS NULL OR u.department_code = '' OR u.department_code = 'NONE')))");
        appendExclude(sql, params);
        sql.append(" ORDER BY u.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByBuildingSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT u.* ");
        sql.append("FROM users u ");
        sql.append("INNER JOIN units unit ON unit.id = u.unit_id ");
        sql.append("LEFT JOIN buildings b ON b.id = unit.building_id ");
        sql.append("WHERE u.status = 'ACTIVE' ");
        sql.append("AND (CAST(b.id AS CHAR) = #{targetValue} ");
        sql.append("OR b.name = #{targetValue} ");
        sql.append("OR unit.unit_no LIKE CONCAT(#{targetValue}, '%'))");
        appendExclude(sql, params);
        sql.append(" ORDER BY u.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByDueBillSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT u.* ");
        sql.append("FROM users u ");
        sql.append("INNER JOIN fee_bills f ON f.unit_id = u.unit_id ");
        sql.append("WHERE u.status = 'ACTIVE' ");
        sql.append("AND f.status IN ('UNPAID', 'PARTIAL', 'OVERDUE')");
        appendExclude(sql, params);
        sql.append(" ORDER BY u.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByCompletedWorkOrderSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT u.* ");
        sql.append("FROM users u ");
        sql.append("INNER JOIN work_orders w ON w.unit_id = u.unit_id ");
        sql.append("WHERE u.status = 'ACTIVE' ");
        sql.append("AND w.status IN ('DONE', 'CLOSED')");
        appendExclude(sql, params);
        sql.append(" ORDER BY u.id ASC");
        return sql.toString();
    }

    private void appendExclude(StringBuilder sql, Map<String, Object> params) {
        if (params.get("excludeUserId") != null) {
            sql.append(" AND u.id <> #{excludeUserId}");
        }
    }
}
