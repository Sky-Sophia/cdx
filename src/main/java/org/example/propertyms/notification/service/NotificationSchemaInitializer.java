package org.example.propertyms.notification.service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
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
        createNotificationBatchTableIfMissing();
        createNotificationTableIfMissing();
        alignNotificationForeignKeyColumnTypes();
        migrateLegacyNotificationSchema();
        createDepartmentTableIfMissing();
        seedDepartments();
        addUsersColumnIfMissing("unit_id", "BIGINT NULL");
        addUsersColumnIfMissing("department_code", "VARCHAR(32) NULL");
        normalizeUserDepartments();
    }

    private void createNotificationBatchTableIfMissing() {
        String userIdType = getColumnType("users", "id", "BIGINT");
        String batchIdType = getColumnType("notification_batches", "id", userIdType);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_batches (
                    id %s NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    batch_no VARCHAR(32) NOT NULL,
                    msg_type VARCHAR(32) NOT NULL,
                    content VARCHAR(500) NOT NULL,
                    sender_id %s NOT NULL,
                    target_type VARCHAR(32) NOT NULL,
                    target_value VARCHAR(128) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_notification_batches_batch_no (batch_no)
                )
                """.formatted(batchIdType, userIdType));
        createIndexIfMissing("idx_notification_batches_sender_id",
                "CREATE INDEX idx_notification_batches_sender_id ON notification_batches (sender_id)");
        createForeignKeyIfMissing("notification_batches", "fk_notification_batches_sender",
                """
                ALTER TABLE notification_batches
                ADD CONSTRAINT fk_notification_batches_sender
                FOREIGN KEY (sender_id) REFERENCES users(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);
    }

    private void createNotificationTableIfMissing() {
        String messageIdType = getColumnType("notification_messages", "id", "BIGINT");
        String batchIdType = getColumnType("notification_batches", "id", "BIGINT");
        String userIdType = getColumnType("users", "id", "BIGINT");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS notification_messages (
                    id %s NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    batch_id %s NOT NULL,
                    receiver_id %s NOT NULL,
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
                """.formatted(messageIdType, batchIdType, userIdType));
        createIndexIfMissing("idx_notification_receiver_deleted",
                "CREATE INDEX idx_notification_receiver_deleted ON notification_messages (receiver_id, is_deleted, send_time)");
        createIndexIfMissing("idx_notification_receiver_unread",
                "CREATE INDEX idx_notification_receiver_unread ON notification_messages (receiver_id, is_deleted, is_read)");
        createIndexIfMissing("idx_notification_batch_id",
                "CREATE INDEX idx_notification_batch_id ON notification_messages (batch_id)");
        createForeignKeyIfMissing("notification_messages", "fk_notification_messages_batch",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_messages_batch
                FOREIGN KEY (batch_id) REFERENCES notification_batches(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);
        createForeignKeyIfMissing("notification_messages", "fk_notification_receiver",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_receiver
                FOREIGN KEY (receiver_id) REFERENCES users(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);
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
                    WHEN role IN ('SUPER_ADMIN', 'OFFICE') THEN 'OFFICE'
                    WHEN role IN ('ADMIN', 'MANAGEMENT') THEN 'MANAGEMENT'
                    WHEN role IN ('ACCOUNTANT', 'FINANCE') THEN 'FINANCE'
                    WHEN role IN ('ENGINEER', 'ENGINEERING', 'STAFF') THEN 'ENGINEERING'
                    ELSE NULL
                END
                WHERE role IN ('SUPER_ADMIN', 'OFFICE', 'ADMIN', 'MANAGEMENT', 'ACCOUNTANT', 'FINANCE',
                               'ENGINEER', 'ENGINEERING', 'STAFF', 'RESIDENT', 'USER')
                  AND (
                      department_code IS NULL
                      OR department_code = ''
                      OR department_code IN ('ADMINISTRATION', 'SECURITY', 'NONE')
                      OR department_code NOT IN ('OFFICE', 'MANAGEMENT', 'FINANCE', 'ENGINEERING')
                  )
                """);
    }

    private void migrateLegacyNotificationSchema() {
        if (!tableExists("notification_messages")) {
            return;
        }
        boolean hasLegacyPayloadColumns = columnExists("notification_messages", "batch_no")
                && columnExists("notification_messages", "msg_type")
                && columnExists("notification_messages", "content")
                && columnExists("notification_messages", "sender_id")
                && columnExists("notification_messages", "target_type");

        String batchIdType = getColumnType("notification_batches", "id", "BIGINT");
        if (!columnExists("notification_messages", "batch_id")) {
            jdbcTemplate.execute("ALTER TABLE notification_messages ADD COLUMN batch_id " + batchIdType + " NULL AFTER id");
        }

        if (hasLegacyPayloadColumns) {
            jdbcTemplate.update("""
                    INSERT INTO notification_batches (
                        batch_no, msg_type, content, sender_id, target_type, target_value, created_at, updated_at
                    )
                    SELECT nm.batch_no,
                           nm.msg_type,
                           nm.content,
                           nm.sender_id,
                           nm.target_type,
                           nm.target_value,
                           MIN(nm.created_at),
                           MAX(nm.updated_at)
                    FROM notification_messages nm
                    LEFT JOIN notification_batches nb ON nb.batch_no = nm.batch_no
                    WHERE nb.id IS NULL
                    GROUP BY nm.batch_no, nm.msg_type, nm.content, nm.sender_id, nm.target_type, nm.target_value
                    """);
            jdbcTemplate.update("""
                    UPDATE notification_messages nm
                    INNER JOIN notification_batches nb ON nb.batch_no = nm.batch_no
                    SET nm.batch_id = nb.id
                    WHERE nm.batch_id IS NULL
                    """);
        }

        Integer missingBatchId = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notification_messages
                WHERE batch_id IS NULL
                """, Integer.class);
        if (missingBatchId != null && missingBatchId == 0) {
            jdbcTemplate.execute("ALTER TABLE notification_messages MODIFY COLUMN batch_id " + batchIdType + " NOT NULL");
        }

        createIndexIfMissing("idx_notification_batch_id",
                "CREATE INDEX idx_notification_batch_id ON notification_messages (batch_id)");
        createForeignKeyIfMissing("notification_messages", "fk_notification_messages_batch",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_messages_batch
                FOREIGN KEY (batch_id) REFERENCES notification_batches(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);
        createForeignKeyIfMissing("notification_messages", "fk_notification_receiver",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_receiver
                FOREIGN KEY (receiver_id) REFERENCES users(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);

        if (columnExists("notification_messages", "sender_id")) {
            dropConstraintIfExists("notification_messages", "fk_notification_sender");
        }
        dropNotificationColumnIfExists("sender_name");
        dropNotificationColumnIfExists("receiver_name");
        dropNotificationColumnIfExists("batch_no");
        dropNotificationColumnIfExists("msg_type");
        dropNotificationColumnIfExists("content");
        dropNotificationColumnIfExists("sender_id");
        dropNotificationColumnIfExists("target_type");
        dropNotificationColumnIfExists("target_value");
    }

    private void alignNotificationForeignKeyColumnTypes() {
        alignReferenceColumnType("notification_batches", "sender_id", "users", "id",
                "fk_notification_batches_sender");
        alignReferenceColumnType("notification_messages", "batch_id", "notification_batches", "id",
                "fk_notification_messages_batch");
        alignReferenceColumnType("notification_messages", "receiver_id", "users", "id",
                "fk_notification_receiver");
    }

    private void addUsersColumnIfMissing(String columnName, String definition) {
        if (columnExists("users", columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN " + columnName + " " + definition);
    }

    private void dropNotificationColumnIfExists(String columnName) {
        if (!columnExists("notification_messages", columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE notification_messages DROP COLUMN " + columnName);
    }

    private void createForeignKeyIfMissing(String tableName, String constraintName, String ddl) {
        if (constraintExists(tableName, constraintName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private void dropConstraintIfExists(String tableName, String constraintName) {
        if (!constraintExists(tableName, constraintName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
    }

    private void createIndexIfMissing(String indexName, String ddl) {
        if (indexExists(indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private void alignReferenceColumnType(String tableName,
                                          String columnName,
                                          String referencedTable,
                                          String referencedColumn,
                                          String foreignKeyName) {
        if (!tableExists(tableName) || !tableExists(referencedTable) || !columnExists(tableName, columnName)) {
            return;
        }
        String currentType = getColumnType(tableName, columnName, null);
        String referencedType = getColumnType(referencedTable, referencedColumn, null);
        if (currentType == null || referencedType == null || currentType.equalsIgnoreCase(referencedType)) {
            return;
        }
        dropConstraintIfExists(tableName, foreignKeyName);
        String nullableSql = isNullable(tableName, columnName) ? " NULL" : " NOT NULL";
        jdbcTemplate.execute("ALTER TABLE " + tableName
                + " MODIFY COLUMN " + columnName + " " + referencedType + nullableSql);
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = metaData.getColumns(
                    connection.getCatalog(),
                    null,
                    tableName.toUpperCase(),
                    columnName.toUpperCase())) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect column: " + tableName + "." + columnName, ex);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private String getColumnType(String tableName, String columnName, String defaultType) {
        String columnType = jdbcTemplate.query("""
                SELECT COLUMN_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, rs -> rs.next() ? rs.getString(1) : null, tableName, columnName);
        return columnType != null ? columnType : defaultType;
    }

    private boolean isNullable(String tableName, String columnName) {
        String nullable = jdbcTemplate.query("""
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, rs -> rs.next() ? rs.getString(1) : "YES", tableName, columnName);
        return !"NO".equalsIgnoreCase(nullable);
    }

    private boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return hasIndex(metaData, connection.getCatalog(), "notification_messages", indexName)
                    || hasIndex(metaData, connection.getCatalog(), "notification_batches", indexName);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect notification index: " + indexName, ex);
        }
    }

    private boolean hasIndex(DatabaseMetaData metaData, String catalog, String tableName, String indexName)
            throws SQLException {
        try (ResultSet resultSet = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metaData.getIndexInfo(catalog, null, tableName.toUpperCase(), false, false)) {
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}

