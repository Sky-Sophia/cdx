-- 物业系统数据库结构优化脚本
-- 适用：MySQL 8.x
-- 执行前请先备份数据库

USE archive_db;

-- 1) 补齐 units.owner_resident_id，并回填当前有效业主
SET @owner_resident_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'units'
      AND COLUMN_NAME = 'owner_resident_id'
);
SET @sql = IF(
    @owner_resident_column_exists = 0,
    'ALTER TABLE units ADD COLUMN owner_resident_id BIGINT NULL AFTER occupancy_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE units u
INNER JOIN (
    SELECT r.unit_id, MAX(r.id) AS owner_resident_id
    FROM residents r
    WHERE r.resident_type = 'OWNER'
      AND r.status = 'ACTIVE'
    GROUP BY r.unit_id
) owners ON owners.unit_id = u.id
SET u.owner_resident_id = owners.owner_resident_id
WHERE u.owner_resident_id IS NULL;

-- 2) 规范枚举/状态值，清洗 users / fee_bills 关键关联数据
UPDATE fee_bills
SET status = CASE UPPER(TRIM(COALESCE(status, '')))
    WHEN 'UNPAID' THEN 'UNPAID'
    WHEN 'PARTIAL' THEN 'PARTIAL'
    WHEN 'OVERDUE' THEN 'OVERDUE'
    WHEN 'PAID' THEN 'PAID'
    ELSE 'UNPAID'
END;

UPDATE residents
SET resident_type = CASE UPPER(TRIM(COALESCE(resident_type, '')))
    WHEN 'OWNER' THEN 'OWNER'
    WHEN 'TENANT' THEN 'TENANT'
    WHEN 'FAMILY' THEN 'TENANT'
    ELSE 'OWNER'
END,
    status = CASE UPPER(TRIM(COALESCE(status, '')))
        WHEN 'ACTIVE' THEN 'ACTIVE'
        WHEN 'MOVED_OUT' THEN 'MOVED_OUT'
        ELSE 'ACTIVE'
    END;

UPDATE units
SET occupancy_status = CASE UPPER(TRIM(COALESCE(occupancy_status, '')))
    WHEN 'OCCUPIED' THEN 'OCCUPIED'
    WHEN 'RENTED' THEN 'RENTED'
    WHEN 'VACANT' THEN 'VACANT'
    ELSE 'VACANT'
END;

UPDATE users
SET status = CASE UPPER(TRIM(COALESCE(status, '')))
    WHEN 'ACTIVE' THEN 'ACTIVE'
    WHEN 'DISABLED' THEN 'DISABLED'
    ELSE 'ACTIVE'
END;

UPDATE users
SET role = CASE UPPER(TRIM(COALESCE(role, '')))
    WHEN 'OFFICE' THEN 'OFFICE'
    WHEN 'ADMIN' THEN 'OFFICE'
    WHEN 'MANAGEMENT' THEN 'MANAGEMENT'
    WHEN 'FINANCE' THEN 'MANAGEMENT'
    WHEN 'ENGINEERING' THEN 'ENGINEERING'
    WHEN 'STAFF' THEN 'ENGINEERING'
    ELSE 'USER'
END;

UPDATE users
SET department_code = CASE
    WHEN role = 'OFFICE' THEN 'OFFICE'
    WHEN role = 'MANAGEMENT' THEN 'MANAGEMENT'
    WHEN role = 'ENGINEERING' THEN 'ENGINEERING'
    ELSE 'NONE'
END
WHERE department_code IS NULL
   OR TRIM(COALESCE(department_code, '')) = ''
   OR department_code IN ('ADMINISTRATION', 'FINANCE', 'SECURITY')
   OR department_code NOT IN ('OFFICE', 'MANAGEMENT', 'ENGINEERING', 'NONE');

UPDATE users
SET department_code = 'NONE'
WHERE role = 'USER';

UPDATE users
SET unit_id = NULL
WHERE unit_id IS NOT NULL
  AND role <> 'USER';

UPDATE users u
LEFT JOIN units unit_ref ON unit_ref.id = u.unit_id
SET u.unit_id = NULL
WHERE u.unit_id IS NOT NULL
  AND unit_ref.id IS NULL;

UPDATE fee_bills
SET billing_month = CASE
    WHEN billing_month IS NULL OR TRIM(billing_month) = ''
        THEN DATE_FORMAT(COALESCE(due_date, DATE(created_at), CURRENT_DATE), '%Y-%m-01')
    WHEN TRIM(billing_month) REGEXP '^[0-9]{4}-[0-9]{2}$'
        THEN CONCAT(TRIM(billing_month), '-01')
    WHEN TRIM(billing_month) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
        THEN DATE_FORMAT(STR_TO_DATE(TRIM(billing_month), '%Y-%m-%d'), '%Y-%m-01')
    ELSE DATE_FORMAT(COALESCE(due_date, DATE(created_at), CURRENT_DATE), '%Y-%m-01')
END;

UPDATE work_orders
SET status = CASE UPPER(TRIM(COALESCE(status, '')))
    WHEN 'OPEN' THEN 'OPEN'
    WHEN 'IN_PROGRESS' THEN 'IN_PROGRESS'
    WHEN 'DONE' THEN 'DONE'
    WHEN 'CLOSED' THEN 'CLOSED'
    ELSE 'OPEN'
END,
    priority = CASE UPPER(TRIM(COALESCE(priority, '')))
        WHEN 'LOW' THEN 'LOW'
        WHEN 'MEDIUM' THEN 'MEDIUM'
        WHEN 'HIGH' THEN 'HIGH'
        WHEN 'URGENT' THEN 'URGENT'
        ELSE 'MEDIUM'
    END;

-- 3) 列类型优化：BIGINT -> INT，billing_month -> DATE，TIMESTAMP -> DATETIME
ALTER TABLE buildings
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE residents
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN unit_id INT NOT NULL,
    MODIFY COLUMN resident_type ENUM('OWNER', 'TENANT') NOT NULL DEFAULT 'OWNER',
    MODIFY COLUMN status ENUM('ACTIVE', 'MOVED_OUT') NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE units
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN building_id INT NOT NULL,
    MODIFY COLUMN occupancy_status ENUM('OCCUPIED', 'RENTED', 'VACANT') NOT NULL DEFAULT 'VACANT',
    MODIFY COLUMN owner_resident_id INT NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE fee_bills
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN unit_id INT NOT NULL,
    MODIFY COLUMN billing_month DATE NOT NULL,
    MODIFY COLUMN status ENUM('UNPAID', 'PARTIAL', 'OVERDUE', 'PAID') NOT NULL DEFAULT 'UNPAID',
    MODIFY COLUMN paid_at DATETIME NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE users
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN unit_id INT NULL,
    MODIFY COLUMN department_code VARCHAR(32) NULL,
    MODIFY COLUMN status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE work_orders
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN unit_id INT NOT NULL,
    MODIFY COLUMN priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    MODIFY COLUMN status ENUM('OPEN', 'IN_PROGRESS', 'DONE', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    MODIFY COLUMN scheduled_at DATETIME NULL,
    MODIFY COLUMN finished_at DATETIME NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE notification_messages
    MODIFY COLUMN send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN read_time DATETIME NULL DEFAULT NULL,
    MODIFY COLUMN deleted_time DATETIME NULL DEFAULT NULL,
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE system_departments
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 4) 删除 units 中冗余业主快照字段，保持 3NF
ALTER TABLE units
    DROP COLUMN owner_name,
    DROP COLUMN owner_phone;

-- 5) 条件删除旧外键/约束，准备重建
SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fee_bills'
      AND CONSTRAINT_NAME = 'fk_fee_bills_unit'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE fee_bills DROP FOREIGN KEY fk_fee_bills_unit', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'residents'
      AND CONSTRAINT_NAME = 'fk_residents_unit'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE residents DROP FOREIGN KEY fk_residents_unit', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'units'
      AND CONSTRAINT_NAME = 'fk_units_building'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE units DROP FOREIGN KEY fk_units_building', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'units'
      AND CONSTRAINT_NAME = 'fk_units_owner_resident'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE units DROP FOREIGN KEY fk_units_owner_resident', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'work_orders'
      AND CONSTRAINT_NAME = 'fk_work_orders_unit'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE work_orders DROP FOREIGN KEY fk_work_orders_unit', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND CONSTRAINT_NAME = 'fk_users_unit'
);
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE users DROP FOREIGN KEY fk_users_unit', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @check_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND CONSTRAINT_NAME = 'chk_users_unit_department_exclusive'
);
SET @sql = IF(@check_exists > 0, 'ALTER TABLE users DROP CHECK chk_users_unit_department_exclusive', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6) 重建外键与互斥约束
ALTER TABLE units
    ADD CONSTRAINT fk_units_building
        FOREIGN KEY (building_id) REFERENCES buildings(id),
    ADD CONSTRAINT fk_units_owner_resident
        FOREIGN KEY (owner_resident_id) REFERENCES residents(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

ALTER TABLE residents
    ADD CONSTRAINT fk_residents_unit
        FOREIGN KEY (unit_id) REFERENCES units(id);

ALTER TABLE fee_bills
    ADD CONSTRAINT fk_fee_bills_unit
        FOREIGN KEY (unit_id) REFERENCES units(id);

ALTER TABLE work_orders
    ADD CONSTRAINT fk_work_orders_unit
        FOREIGN KEY (unit_id) REFERENCES units(id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_unit
        FOREIGN KEY (unit_id) REFERENCES units(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    ADD CONSTRAINT chk_users_unit_department_exclusive
        CHECK (
            (role = 'OFFICE' AND unit_id IS NULL AND department_code = 'OFFICE')
            OR (role = 'MANAGEMENT' AND unit_id IS NULL AND department_code = 'MANAGEMENT')
            OR (role = 'ENGINEERING' AND unit_id IS NULL AND department_code = 'ENGINEERING')
            OR (role = 'USER' AND (department_code IS NULL OR department_code = 'NONE'))
        );

-- 7) 补充高频查询索引
CREATE INDEX idx_units_status_building_updated
    ON units (occupancy_status, building_id, updated_at, id);

CREATE INDEX idx_residents_status_moveout_unit
    ON residents (status, move_out_date, unit_id);

CREATE INDEX idx_fee_bills_status_due_date
    ON fee_bills (status, due_date, id);

CREATE INDEX idx_fee_bills_billing_month_status
    ON fee_bills (billing_month, status);

CREATE INDEX idx_work_orders_status_priority_created
    ON work_orders (status, priority, created_at, id);

CREATE INDEX idx_users_status_role_created
    ON users (status, role, created_at, id);
