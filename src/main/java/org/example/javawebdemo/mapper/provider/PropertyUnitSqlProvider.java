package org.example.javawebdemo.mapper.provider;

import java.util.Map;

public class PropertyUnitSqlProvider {

    public String findAllSql(Map<String, Object> params) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.*, b.name AS building_name ");
        sql.append("FROM units u ");
        sql.append("LEFT JOIN buildings b ON b.id = u.building_id ");
        sql.append("WHERE 1=1");

        if (isNotBlank(params.get("keyword"))) {
            sql.append(" AND (u.unit_no LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.owner_name LIKE CONCAT('%', #{keyword}, '%')");
            sql.append(" OR u.owner_phone LIKE CONCAT('%', #{keyword}, '%'))");
        }
        if (params.get("buildingId") != null) {
            sql.append(" AND u.building_id = #{buildingId}");
        }
        if (isNotBlank(params.get("status"))) {
            sql.append(" AND u.occupancy_status = #{status}");
        }

        sql.append(" ORDER BY u.updated_at DESC, u.id DESC");
        return sql.toString();
    }

    private boolean isNotBlank(Object value) {
        return value instanceof String str && !str.isBlank();
    }
}
