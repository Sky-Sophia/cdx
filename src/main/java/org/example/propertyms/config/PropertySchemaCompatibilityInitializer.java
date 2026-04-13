package org.example.propertyms.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PropertySchemaCompatibilityInitializer {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PropertySchemaCompatibilityInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        migrateResidentsSchema();
        migrateUnitOwnerRelation();
        migrateUserAccountsSchema();
        migrateFeeBillsSchema();
    }

    private void migrateResidentsSchema() {
        if (!tableExists("residents")) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE residents
                SET resident_type = CASE UPPER(TRIM(COALESCE(resident_type, '')))
                    WHEN 'OWNER' THEN 'OWNER'
                    WHEN 'TENANT' THEN 'TENANT'
                    WHEN 'FAMILY' THEN 'TENANT'
                    ELSE 'OWNER'
                END
                """);
    }

    private void migrateUnitOwnerRelation() {
        if (!tableExists("units") || !tableExists("residents")) {
            return;
        }
        addOwnerResidentColumnIfMissing();
        backfillOwnerResident();
        createForeignKeyIfMissing("units", "fk_units_owner_resident", """
                ALTER TABLE units
                ADD CONSTRAINT fk_units_owner_resident
                FOREIGN KEY (owner_resident_id) REFERENCES residents(id)
                ON DELETE SET NULL
                ON UPDATE CASCADE
                """);
    }

    private void migrateUserAccountsSchema() {
        if (!tableExists("user_accounts")) {
            return;
        }
        normalizeUserAccounts();
        normalizeEmployeeAccounts();
    }

    private void migrateFeeBillsSchema() {
        if (!tableExists("fee_bills") || !columnExists("fee_bills", "billing_month")) {
            return;
        }
        String billingMonthType = getColumnTypeName("fee_bills", "billing_month");
        if (billingMonthType == null) {
            return;
        }
        String normalizedType = billingMonthType.toUpperCase();
        if (!"DATE".equals(normalizedType)) {
            if (normalizedType.contains("CHAR") || normalizedType.contains("TEXT")) {
                jdbcTemplate.execute("""
                        ALTER TABLE fee_bills
                        MODIFY COLUMN billing_month VARCHAR(10) NOT NULL
                        """);
                normalizeFeeBillBillingMonthValues();
            }
            jdbcTemplate.execute("""
                    ALTER TABLE fee_bills
                    MODIFY COLUMN billing_month DATE NOT NULL
                    """);
        }
        createIndexIfMissing("fee_bills", "idx_fee_bills_billing_month_status", """
                CREATE INDEX idx_fee_bills_billing_month_status
                ON fee_bills (billing_month, status)
                """);
    }

    private void addOwnerResidentColumnIfMissing() {
        if (columnExists("units", "owner_resident_id")) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE units ADD COLUMN owner_resident_id BIGINT NULL AFTER occupancy_status");
    }

    private void backfillOwnerResident() {
        jdbcTemplate.update("""
                UPDATE units u
                INNER JOIN (
                    SELECT r.unit_id, MAX(r.id) AS owner_resident_id
                    FROM residents r
                    WHERE r.resident_type = 'OWNER'
                      AND r.status = 'ACTIVE'
                    GROUP BY r.unit_id
                ) owners ON owners.unit_id = u.id
                SET u.owner_resident_id = owners.owner_resident_id
                WHERE u.owner_resident_id IS NULL
                """);
    }

    private void normalizeUserAccounts() {
        jdbcTemplate.update("""
                UPDATE user_accounts
                SET account_role = CASE UPPER(TRIM(COALESCE(account_role, '')))
                    WHEN 'SUPER_ADMIN' THEN 'SUPER_ADMIN'
                    WHEN 'OFFICE' THEN 'SUPER_ADMIN'
                    WHEN 'ADMIN' THEN 'ADMIN'
                    WHEN 'MANAGEMENT' THEN 'ADMIN'
                    WHEN 'ACCOUNTANT' THEN 'ACCOUNTANT'
                    WHEN 'FINANCE' THEN 'ACCOUNTANT'
                    WHEN 'ENGINEER' THEN 'ENGINEER'
                    WHEN 'ENGINEERING' THEN 'ENGINEER'
                    WHEN 'STAFF' THEN 'ENGINEER'
                    ELSE 'RESIDENT'
                END
                """);
        jdbcTemplate.update("""
                UPDATE user_accounts
                SET account_type = CASE
                    WHEN account_role = 'RESIDENT' THEN 'RESIDENT'
                    ELSE 'EMPLOYEE'
                END
                WHERE account_type IS NULL
                   OR account_type = ''
                   OR (account_role = 'RESIDENT' AND account_type <> 'RESIDENT')
                   OR (account_role <> 'RESIDENT' AND account_type <> 'EMPLOYEE')
                """);
    }

    private void normalizeEmployeeAccounts() {
        if (!tableExists("employees")) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE employees e
                INNER JOIN user_accounts ua ON ua.id = e.account_id
                SET e.employee_role = ua.account_role,
                    e.department_code = CASE
                        WHEN ua.account_role = 'SUPER_ADMIN' THEN 'OFFICE'
                        WHEN ua.account_role = 'ADMIN' THEN 'MANAGEMENT'
                        WHEN ua.account_role = 'ACCOUNTANT' THEN 'FINANCE'
                        WHEN ua.account_role = 'ENGINEER' THEN 'ENGINEERING'
                        ELSE e.department_code
                    END,
                    e.status = ua.status,
                    e.updated_at = CURRENT_TIMESTAMP
                """);
    }

    private void normalizeFeeBillBillingMonthValues() {
        jdbcTemplate.update("""
                UPDATE fee_bills
                SET billing_month = CASE
                    WHEN billing_month IS NULL OR TRIM(billing_month) = ''
                        THEN DATE_FORMAT(COALESCE(due_date, DATE(created_at), CURRENT_DATE), '%Y-%m-01')
                    WHEN TRIM(billing_month) REGEXP '^[0-9]{4}-[0-9]{2}$'
                        THEN CONCAT(TRIM(billing_month), '-01')
                    WHEN TRIM(billing_month) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'
                        THEN DATE_FORMAT(STR_TO_DATE(TRIM(billing_month), '%Y-%m-%d'), '%Y-%m-01')
                    ELSE DATE_FORMAT(COALESCE(due_date, DATE(created_at), CURRENT_DATE), '%Y-%m-01')
                END
                """);
    }

    private void createForeignKeyIfMissing(String tableName, String foreignKeyName, String ddl) {
        if (foreignKeyExists(tableName, foreignKeyName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private void createCheckConstraintIfMissing(String tableName, String constraintName, String ddl) {
        if (constraintExists(tableName, constraintName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private void createIndexIfMissing(String tableName, String indexName, String ddl) {
        if (indexExists(tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, null)) {
                if (resultSet.next()) {
                    return true;
                }
            }
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName.toUpperCase(), null)) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect table: " + tableName, ex);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        return getColumnTypeName(tableName, columnName) != null;
    }

    private String getColumnTypeName(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (resultSet.next()) {
                    return resultSet.getString("TYPE_NAME");
                }
            }
            try (ResultSet resultSet = metaData.getColumns(
                    connection.getCatalog(),
                    null,
                    tableName.toUpperCase(),
                    columnName.toUpperCase())) {
                if (resultSet.next()) {
                    return resultSet.getString("TYPE_NAME");
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect column: " + tableName + "." + columnName, ex);
        }
    }

    private boolean foreignKeyExists(String tableName, String foreignKeyName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getImportedKeys(connection.getCatalog(), null, tableName)) {
                while (resultSet.next()) {
                    if (foreignKeyName.equalsIgnoreCase(resultSet.getString("FK_NAME"))) {
                        return true;
                    }
                }
            }
            try (ResultSet resultSet = metaData.getImportedKeys(connection.getCatalog(), null, tableName.toUpperCase())) {
                while (resultSet.next()) {
                    if (foreignKeyName.equalsIgnoreCase(resultSet.getString("FK_NAME"))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect foreign key: " + tableName + "." + foreignKeyName, ex);
        }
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
            throw new IllegalStateException("Failed to inspect index: " + tableName + "." + indexName, ex);
        }
    }
}

