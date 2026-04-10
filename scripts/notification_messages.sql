CREATE TABLE IF NOT EXISTS notification_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(32) NOT NULL,
    msg_type VARCHAR(32) NOT NULL,
    content VARCHAR(500) NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(64) NOT NULL,
    receiver_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    send_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read TINYINT NOT NULL DEFAULT 0,
    read_time TIMESTAMP NULL DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    deleted_time TIMESTAMP NULL DEFAULT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_value VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD COLUMN unit_id BIGINT NULL;
ALTER TABLE users ADD COLUMN department_code VARCHAR(32) NULL;

CREATE INDEX idx_notification_receiver_deleted ON notification_messages (receiver_id, is_deleted, send_time);
CREATE INDEX idx_notification_receiver_unread ON notification_messages (receiver_id, is_deleted, is_read);
CREATE INDEX idx_notification_batch_no ON notification_messages (batch_no);
