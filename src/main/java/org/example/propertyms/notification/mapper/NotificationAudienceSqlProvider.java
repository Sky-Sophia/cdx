package org.example.propertyms.notification.mapper;

import java.util.Map;

public class NotificationAudienceSqlProvider {

    public String findAllActiveUsersSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder(NotificationAudienceMapper.ACTIVE_USER_SELECT);
        sql.append(" WHERE ua.status = 'ACTIVE'");
        appendExclude(sql, params);
        sql.append(" ORDER BY ua.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByDepartmentSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append(NotificationAudienceMapper.ACTIVE_USER_SELECT);
        sql.append(" WHERE ua.status = 'ACTIVE'");
        sql.append(" AND (employee_link.department_code = #{departmentCode}");
        sql.append(" OR ((employee_link.department_code IS NULL OR employee_link.department_code = '')");
        sql.append(" AND ua.account_role = #{fallbackRole}))");
        appendExclude(sql, params);
        sql.append(" ORDER BY ua.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByBuildingSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ua.id, ua.username, ua.password_hash AS password, ua.account_role AS role, ");
        sql.append("ua.status, resident_link.unit_id AS unit_id, employee_link.department_code AS department_code, ");
        sql.append("ua.created_at, ua.updated_at ");
        sql.append("FROM user_accounts ua ");
        sql.append("INNER JOIN residents resident_link ON resident_link.account_id = ua.id AND resident_link.status = 'ACTIVE' ");
        sql.append("LEFT JOIN employees employee_link ON employee_link.account_id = ua.id ");
        sql.append("INNER JOIN units unit ON unit.id = resident_link.unit_id ");
        sql.append("LEFT JOIN buildings b ON b.id = unit.building_id ");
        sql.append("WHERE ua.status = 'ACTIVE' ");
        sql.append("AND (CAST(b.id AS CHAR) = #{targetValue} ");
        sql.append("OR b.name = #{targetValue} ");
        sql.append("OR unit.unit_no LIKE CONCAT(#{targetValue}, '%'))");
        appendExclude(sql, params);
        sql.append(" ORDER BY ua.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByDueBillSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ua.id, ua.username, ua.password_hash AS password, ua.account_role AS role, ");
        sql.append("ua.status, resident_link.unit_id AS unit_id, employee_link.department_code AS department_code, ");
        sql.append("ua.created_at, ua.updated_at ");
        sql.append("FROM user_accounts ua ");
        sql.append("INNER JOIN residents resident_link ON resident_link.account_id = ua.id AND resident_link.status = 'ACTIVE' ");
        sql.append("LEFT JOIN employees employee_link ON employee_link.account_id = ua.id ");
        sql.append("INNER JOIN fee_bills f ON f.unit_id = resident_link.unit_id ");
        sql.append("WHERE ua.status = 'ACTIVE' ");
        sql.append("AND f.status IN ('UNPAID', 'PARTIAL', 'OVERDUE')");
        appendExclude(sql, params);
        sql.append(" ORDER BY ua.id ASC");
        return sql.toString();
    }

    public String findActiveUsersByCompletedWorkOrderSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ua.id, ua.username, ua.password_hash AS password, ua.account_role AS role, ");
        sql.append("ua.status, resident_link.unit_id AS unit_id, employee_link.department_code AS department_code, ");
        sql.append("ua.created_at, ua.updated_at ");
        sql.append("FROM user_accounts ua ");
        sql.append("INNER JOIN residents resident_link ON resident_link.account_id = ua.id AND resident_link.status = 'ACTIVE' ");
        sql.append("LEFT JOIN employees employee_link ON employee_link.account_id = ua.id ");
        sql.append("INNER JOIN work_orders w ON w.unit_id = resident_link.unit_id ");
        sql.append("WHERE ua.status = 'ACTIVE' ");
        sql.append("AND w.status IN ('DONE', 'CLOSED')");
        appendExclude(sql, params);
        sql.append(" ORDER BY ua.id ASC");
        return sql.toString();
    }

    private void appendExclude(StringBuilder sql, Map<String, Object> params) {
        if (params.get("excludeUserId") != null) {
            sql.append(" AND ua.id <> #{excludeUserId}");
        }
    }
}

