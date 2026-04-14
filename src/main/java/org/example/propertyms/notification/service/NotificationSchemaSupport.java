package org.example.propertyms.notification.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知表结构辅助组件，集中处理元数据检查、索引/约束创建与列类型对齐。
 */
@Component
public class NotificationSchemaSupport {
    static final String TABLE_BATCHES = "notification_batches";
    static final String TABLE_MESSAGES = "notification_messages";
    static final String TABLE_DEPARTMENTS = "system_departments";
    static final String TABLE_ACCOUNTS = "user_accounts";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaSupport(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    public boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count == null || count <= 0;
    }

    public boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            if (hasColumn(metaData, connection.getCatalog(), tableName, columnName)) {
                return true;
            }
            return hasColumn(metaData, connection.getCatalog(), tableName.toUpperCase(), columnName.toUpperCase());
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect column: " + tableName + "." + columnName, ex);
        }
    }

    public String getColumnType(String tableName, String columnName, String defaultType) {
        String columnType = jdbcTemplate.query("""
                SELECT COLUMN_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, rs -> rs.next() ? rs.getString(1) : null, tableName, columnName);
        return columnType != null ? columnType : defaultType;
    }

    public boolean isNullable(String tableName, String columnName) {
        String nullable = jdbcTemplate.query("""
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, rs -> rs.next() ? rs.getString(1) : "YES", tableName, columnName);
        return !"NO".equalsIgnoreCase(nullable);
    }

    public boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    public boolean indexExists(String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return hasIndex(metaData, connection.getCatalog(), TABLE_MESSAGES, indexName)
                    || hasIndex(metaData, connection.getCatalog(), TABLE_BATCHES, indexName);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect notification index: " + indexName, ex);
        }
    }

    public void createForeignKeyIfMissing(String tableName, String constraintName, String ddl) {
        if (!constraintExists(tableName, constraintName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    public void dropConstraintIfExists(String tableName, String constraintName) {
        if (constraintExists(tableName, constraintName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
        }
    }

    public void createIndexIfMissing(String indexName, String ddl) {
        if (!indexExists(indexName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    public void dropColumnIfExists(String tableName, String columnName) {
        if (columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
        }
    }

    public void alignReferenceColumnType(String tableName,
                                         String columnName,
                                         String referencedTable,
                                         String referencedColumn,
                                         String foreignKeyName) {
        if (tableExists(tableName) || tableExists(referencedTable) || !columnExists(tableName, columnName)) {
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

    public String requireAccountTable() {
        if (tableExists(TABLE_ACCOUNTS)) {
            throw new IllegalStateException("缺少 user_accounts 表，通知模块无法初始化。");
        }
        return TABLE_ACCOUNTS;
    }

    private boolean hasColumn(DatabaseMetaData metaData, String catalog, String tableName, String columnName)
            throws SQLException {
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, columnName)) {
            return resultSet.next();
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
