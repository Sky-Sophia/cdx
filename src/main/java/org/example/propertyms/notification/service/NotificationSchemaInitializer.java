package org.example.propertyms.notification.service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class NotificationSchemaInitializer {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        createNotificationTableIfMissing();
        createDepartmentTableIfMissing();
        seedDepartments();
        addUsersColumnIfMissing("unit_id", "BIGINT NULL");
        addUsersColumnIfMissing("department_code", "VARCHAR(32) NULL");
        normalizeUserDepartments();
    }

    private void createNotificationTableIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_messages (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    batch_no VARCHAR(32) NOT NULL,
                    msg_type VARCHAR(32) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    sender_id BIGINT NOT NULL,
                    sender_name VARCHAR(64) NOT NULL,
                    receiver_id BIGINT NOT NULL,
                    receiver_name VARCHAR(64) NOT NULL,
                    send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_read TINYINT NOT NULL DEFAULT 0,
                    read_time DATETIME NULL DEFAULT NULL,
                    is_deleted TINYINT NOT NULL DEFAULT 0,
                    deleted_time DATETIME NULL DEFAULT NULL,
                    target_type VARCHAR(32) NOT NULL,
                    target_value VARCHAR(128) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        createIndexIfMissing("notification_messages", "idx_notification_receiver_deleted",
                "CREATE INDEX idx_notification_receiver_deleted ON notification_messages (receiver_id, is_deleted, send_time)");
        createIndexIfMissing("notification_messages", "idx_notification_receiver_unread",
                "CREATE INDEX idx_notification_receiver_unread ON notification_messages (receiver_id, is_deleted, is_read)");
        createIndexIfMissing("notification_messages", "idx_notification_batch_no",
                "CREATE INDEX idx_notification_batch_no ON notification_messages (batch_no)");
    }

    private void createDepartmentTableIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS system_departments (
                    code VARCHAR(32) PRIMARY KEY,
                    label VARCHAR(32) NOT NULL,
                    sort_order INT NOT NULL DEFAULT 0,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    private void seedDepartments() {
        for (NotificationDepartment department : NotificationDepartment.values()) {
            jdbcTemplate.update("""
                            INSERT INTO system_departments (code, label, sort_order, enabled)
                            VALUES (?, ?, ?, 1)
                            ON DUPLICATE KEY UPDATE
                                label = VALUES(label),
                                sort_order = VALUES(sort_order),
                                enabled = VALUES(enabled),
                                updated_at = CURRENT_TIMESTAMP
                            """,
                    department.getCode(),
                    department.getLabel(),
                    department.getSortOrder());
        }
    }

    private void normalizeUserDepartments() {
        jdbcTemplate.update("""
                UPDATE users
                SET department_code = CASE
                    WHEN department_code = 'SECURITY' THEN 'MANAGEMENT'
                    WHEN department_code = 'ADMINISTRATION' AND role = 'ADMIN' THEN 'MANAGEMENT'
                    WHEN department_code IS NULL OR department_code = ''
                         OR department_code NOT IN ('ADMINISTRATION', 'FINANCE', 'ENGINEERING', 'MANAGEMENT', 'NONE')
                    THEN CASE
                        WHEN role = 'ADMIN' THEN 'MANAGEMENT'
                        WHEN role = 'FINANCE' THEN 'FINANCE'
                        WHEN role = 'STAFF' THEN 'ENGINEERING'
                        ELSE 'NONE'
                    END
                    ELSE department_code
                END
                WHERE department_code IS NULL
                   OR department_code = ''
                   OR department_code = 'SECURITY'
                   OR (department_code = 'ADMINISTRATION' AND role = 'ADMIN')
                   OR department_code NOT IN ('ADMINISTRATION', 'FINANCE', 'ENGINEERING', 'MANAGEMENT', 'NONE')
                """);
    }

    private void addUsersColumnIfMissing(String columnName, String definition) {
        if (columnExists("users", columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN " + columnName + " " + definition);
    }

    private void createIndexIfMissing(String tableName, String indexName, String ddl) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("检查数据库列失败: " + tableName + "." + columnName, ex);
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
                while (resultSet.next()) {
                    if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
            }
            try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableName.toUpperCase(), false, false)) {
                while (resultSet.next()) {
                    if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new IllegalStateException("检查数据库索引失败: " + tableName + "." + indexName, ex);
        }
    }
}
