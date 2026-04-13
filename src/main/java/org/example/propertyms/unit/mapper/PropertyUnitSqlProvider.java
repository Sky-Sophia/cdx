package org.example.propertyms.unit.mapper;

import java.util.Map;
import org.example.propertyms.common.util.SqlProviderHelper;

public class PropertyUnitSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(PropertyUnitMapper.BASE_COLUMNS);
        sql.append(' ');
        sql.append("FROM units u ");
        sql.append("LEFT JOIN buildings b ON b.id = u.building_id ");
        sql.append("LEFT JOIN residents r ON r.id = u.owner_resident_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        sql.append(" ORDER BY u.updated_at DESC, u.id DESC");
        return sql.toString();
    }

    public String countSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append("FROM units u ");
        sql.append("LEFT JOIN buildings b ON b.id = u.building_id ");
        sql.append("LEFT JOIN residents r ON r.id = u.owner_resident_id ");
        sql.append("WHERE 1=1");
        appendFilters(sql, params);
        return sql.toString();
    }

    public String findAllPagedSql(Map<String, Object> params) {
        return findAllSql(params) + " LIMIT #{offset}, #{pageSize}";
    }

    private void appendFilters(StringBuilder sql, Map<String, Object> params) {
        if (SqlProviderHelper.isNotBlank(params.get("keyword"))) {
            sql.append(" AND (u.unit_no LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR r.name LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR r.phone LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (params.get("buildingId") != null) {
            sql.append(" AND u.building_id = #{buildingId}");
        }
        if (SqlProviderHelper.isNotBlank(params.get("status"))) {
            sql.append(" AND u.occupancy_status = #{status}");
        }
    }
}

