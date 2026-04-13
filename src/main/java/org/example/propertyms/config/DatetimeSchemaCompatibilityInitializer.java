package org.example.propertyms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@DependsOn({"propertySchemaCompatibilityInitializer", "notificationSchemaInitializer"})
public class DatetimeSchemaCompatibilityInitializer {
    private final JdbcTemplate jdbcTemplate;

    public DatetimeSchemaCompatibilityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        convertPropertyTables();
        convertNotificationTables();
    }

    private void convertPropertyTables() {
        convertTimestampColumnToDatetimeIfNeeded("buildings", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("buildings", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("units", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("units", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("persons", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("persons", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("user_accounts", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("user_accounts", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("employees", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("residents", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("residents", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("fee_bills", "paid_at");
        convertTimestampColumnToDatetimeIfNeeded("fee_bills", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("fee_bills", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("work_orders", "scheduled_at");
        convertTimestampColumnToDatetimeIfNeeded("work_orders", "finished_at");
        convertTimestampColumnToDatetimeIfNeeded("work_orders", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("work_orders", "updated_at");
    }

    private void convertNotificationTables() {
        convertTimestampColumnToDatetimeIfNeeded("notification_batches", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("notification_batches", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("notification_messages", "send_time");
        convertTimestampColumnToDatetimeIfNeeded("notification_messages", "read_time");
        convertTimestampColumnToDatetimeIfNeeded("notification_messages", "deleted_time");
        convertTimestampColumnToDatetimeIfNeeded("notification_messages", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("notification_messages", "updated_at");
        convertTimestampColumnToDatetimeIfNeeded("system_departments", "created_at");
        convertTimestampColumnToDatetimeIfNeeded("system_departments", "updated_at");
    }

    private void convertTimestampColumnToDatetimeIfNeeded(String tableName, String columnName) {
        ColumnDefinition column = loadColumnDefinition(tableName, columnName);
        if (column == null || !"timestamp".equalsIgnoreCase(column.dataType)) {
            return;
        }

        StringBuilder ddl = new StringBuilder();
        ddl.append("ALTER TABLE ").append(tableName)
                .append(" MODIFY COLUMN ").append(columnName)
                .append(" DATETIME");
        ddl.append(column.nullable ? " NULL" : " NOT NULL");

        String defaultValue = normalizeDefaultValue(column.columnDefault);
        if (defaultValue != null) {
            ddl.append(" DEFAULT ").append(defaultValue);
        } else if (column.nullable) {
            ddl.append(" DEFAULT NULL");
        }
        if (column.extra != null && column.extra.toLowerCase().contains("on update current_timestamp")) {
            ddl.append(" ON UPDATE CURRENT_TIMESTAMP");
        }
        jdbcTemplate.execute(ddl.toString());
    }

    private ColumnDefinition loadColumnDefinition(String tableName, String columnName) {
        return jdbcTemplate.query("""
                        SELECT DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        LIMIT 1
                        """,
                ps -> {
                    ps.setString(1, tableName);
                    ps.setString(2, columnName);
                },
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new ColumnDefinition(
                            rs.getString("DATA_TYPE"),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")),
                            rs.getString("COLUMN_DEFAULT"),
                            rs.getString("EXTRA"));
                });
    }

    private String normalizeDefaultValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("current_timestamp")
                || trimmed.equalsIgnoreCase("current_timestamp()")) {
            return "CURRENT_TIMESTAMP";
        }
        return "'" + trimmed.replace("'", "''") + "'";
    }

    private record ColumnDefinition(String dataType, boolean nullable, String columnDefault, String extra) {
    }
}

