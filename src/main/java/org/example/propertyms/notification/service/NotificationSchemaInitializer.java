package org.example.propertyms.notification.service;

import jakarta.annotation.PostConstruct;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class NotificationSchemaInitializer {
    private final NotificationSchemaSupport schemaSupport;

    public NotificationSchemaInitializer(NotificationSchemaSupport schemaSupport) {
        this.schemaSupport = schemaSupport;
    }

    @PostConstruct
    public void initialize() {
        createNotificationBatchTableIfMissing();
        createNotificationMessageTableIfMissing();
        alignNotificationForeignKeyColumnTypes();
        migrateLegacyNotificationSchema();
        alignNotificationPopupColumns();
        createDepartmentTableIfMissing();
        seedDepartments();
    }

    private void createNotificationBatchTableIfMissing() {
        String accountTable = schemaSupport.requireAccountTable();
        String accountIdType = schemaSupport.getColumnType(accountTable, "id", "BIGINT");
        String batchIdType = schemaSupport.getColumnType(NotificationSchemaSupport.TABLE_BATCHES, "id", accountIdType);
        jdbcTemplate().execute("""
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
                """.formatted(batchIdType, accountIdType));
        schemaSupport.createIndexIfMissing("idx_notification_batches_sender_id",
                "CREATE INDEX idx_notification_batches_sender_id ON notification_batches (sender_id)");
        schemaSupport.createForeignKeyIfMissing(NotificationSchemaSupport.TABLE_BATCHES, "fk_notification_batches_sender",
                """
                ALTER TABLE notification_batches
                ADD CONSTRAINT fk_notification_batches_sender
                FOREIGN KEY (sender_id) REFERENCES %s(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """.formatted(accountTable));
    }

    private void createNotificationMessageTableIfMissing() {
        String messageIdType = schemaSupport.getColumnType(NotificationSchemaSupport.TABLE_MESSAGES, "id", "BIGINT");
        String batchIdType = schemaSupport.getColumnType(NotificationSchemaSupport.TABLE_BATCHES, "id", "BIGINT");
        String accountTable = schemaSupport.requireAccountTable();
        String accountIdType = schemaSupport.getColumnType(accountTable, "id", "BIGINT");
        jdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS notification_messages (
                    id %s NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    batch_id %s NOT NULL,
                    receiver_id %s NOT NULL,
                    send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_read TINYINT NOT NULL DEFAULT 0,
                    read_time DATETIME NULL DEFAULT NULL,
                    is_deleted TINYINT NOT NULL DEFAULT 0,
                    deleted_time DATETIME NULL DEFAULT NULL,
                    is_popup_hidden TINYINT NOT NULL DEFAULT 0,
                    popup_hidden_time DATETIME NULL DEFAULT NULL,
                    target_type VARCHAR(32) NOT NULL,
                    target_value VARCHAR(128) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(messageIdType, batchIdType, accountIdType));
        ensureMessageIndexes();
        ensureMessageForeignKeys(accountTable);
    }

    private void migrateLegacyNotificationSchema() {
        if (!schemaSupport.tableExists(NotificationSchemaSupport.TABLE_MESSAGES)) {
            return;
        }

        ensureBatchIdColumnExists();
        if (hasLegacyPayloadColumns()) {
            migrateLegacyBatchPayload();
        }
        ensureBatchIdNotNullIfReady();
        ensureMessageIndexes();
        ensureMessageForeignKeys(schemaSupport.requireAccountTable());
        cleanupLegacyColumns();
    }

    private void alignNotificationForeignKeyColumnTypes() {
        String accountTable = schemaSupport.requireAccountTable();
        schemaSupport.alignReferenceColumnType(NotificationSchemaSupport.TABLE_BATCHES, "sender_id", accountTable, "id",
                "fk_notification_batches_sender");
        schemaSupport.alignReferenceColumnType(NotificationSchemaSupport.TABLE_MESSAGES, "batch_id",
                NotificationSchemaSupport.TABLE_BATCHES, "id", "fk_notification_messages_batch");
        schemaSupport.alignReferenceColumnType(NotificationSchemaSupport.TABLE_MESSAGES, "receiver_id", accountTable, "id",
                "fk_notification_receiver");
    }

    private void alignNotificationPopupColumns() {
        if (!schemaSupport.tableExists(NotificationSchemaSupport.TABLE_MESSAGES)) {
            return;
        }
        if (!schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "is_popup_hidden")) {
            jdbcTemplate().execute("""
                    ALTER TABLE notification_messages
                    ADD COLUMN is_popup_hidden TINYINT NOT NULL DEFAULT 0 AFTER deleted_time
                    """);
        }
        if (!schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "popup_hidden_time")) {
            jdbcTemplate().execute("""
                    ALTER TABLE notification_messages
                    ADD COLUMN popup_hidden_time DATETIME NULL DEFAULT NULL AFTER is_popup_hidden
                    """);
        }
        schemaSupport.createIndexIfMissing("idx_notification_receiver_popup",
                "CREATE INDEX idx_notification_receiver_popup ON notification_messages (receiver_id, is_deleted, is_popup_hidden, send_time)");
    }

    private void createDepartmentTableIfMissing() {
        jdbcTemplate().execute("""
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
            jdbcTemplate().update("""
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

    private void ensureBatchIdColumnExists() {
        if (schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "batch_id")) {
            return;
        }
        String batchIdType = schemaSupport.getColumnType(NotificationSchemaSupport.TABLE_BATCHES, "id", "BIGINT");
        jdbcTemplate().execute("ALTER TABLE notification_messages ADD COLUMN batch_id " + batchIdType + " NULL AFTER id");
    }

    private boolean hasLegacyPayloadColumns() {
        return schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "batch_no")
                && schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "msg_type")
                && schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "content")
                && schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "sender_id")
                && schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "target_type");
    }

    private void migrateLegacyBatchPayload() {
        jdbcTemplate().update("""
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
        jdbcTemplate().update("""
                UPDATE notification_messages nm
                INNER JOIN notification_batches nb ON nb.batch_no = nm.batch_no
                SET nm.batch_id = nb.id
                WHERE nm.batch_id IS NULL
                """);
    }

    private void ensureBatchIdNotNullIfReady() {
        Integer missingBatchId = jdbcTemplate().queryForObject("""
                SELECT COUNT(*)
                FROM notification_messages
                WHERE batch_id IS NULL
                """, Integer.class);
        if (missingBatchId != null && missingBatchId == 0) {
            String batchIdType = schemaSupport.getColumnType(NotificationSchemaSupport.TABLE_BATCHES, "id", "BIGINT");
            jdbcTemplate().execute("ALTER TABLE notification_messages MODIFY COLUMN batch_id " + batchIdType + " NOT NULL");
        }
    }

    private void ensureMessageIndexes() {
        schemaSupport.createIndexIfMissing("idx_notification_receiver_deleted",
                "CREATE INDEX idx_notification_receiver_deleted ON notification_messages (receiver_id, is_deleted, send_time)");
        schemaSupport.createIndexIfMissing("idx_notification_receiver_unread",
                "CREATE INDEX idx_notification_receiver_unread ON notification_messages (receiver_id, is_deleted, is_read)");
        schemaSupport.createIndexIfMissing("idx_notification_receiver_popup",
                "CREATE INDEX idx_notification_receiver_popup ON notification_messages (receiver_id, is_deleted, is_popup_hidden, send_time)");
        schemaSupport.createIndexIfMissing("idx_notification_batch_id",
                "CREATE INDEX idx_notification_batch_id ON notification_messages (batch_id)");
    }

    private void ensureMessageForeignKeys(String accountTable) {
        schemaSupport.createForeignKeyIfMissing(NotificationSchemaSupport.TABLE_MESSAGES, "fk_notification_messages_batch",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_messages_batch
                FOREIGN KEY (batch_id) REFERENCES notification_batches(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """);
        schemaSupport.createForeignKeyIfMissing(NotificationSchemaSupport.TABLE_MESSAGES, "fk_notification_receiver",
                """
                ALTER TABLE notification_messages
                ADD CONSTRAINT fk_notification_receiver
                FOREIGN KEY (receiver_id) REFERENCES %s(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE
                """.formatted(accountTable));
    }

    private void cleanupLegacyColumns() {
        if (schemaSupport.columnExists(NotificationSchemaSupport.TABLE_MESSAGES, "sender_id")) {
            schemaSupport.dropConstraintIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "fk_notification_sender");
        }
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "sender_name");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "receiver_name");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "batch_no");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "msg_type");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "content");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "sender_id");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "target_type");
        schemaSupport.dropColumnIfExists(NotificationSchemaSupport.TABLE_MESSAGES, "target_value");
    }

    private JdbcTemplate jdbcTemplate() {
        return schemaSupport.jdbcTemplate();
    }
}
