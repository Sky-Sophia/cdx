# 数据脚本说明

本目录提供物业管理系统的中文示例数据生成脚本。

## 生成内容

执行 `generate_realistic_property_seed.py` 后，会生成一份可直接导入 MySQL 的 SQL 文件，包含以下合成示例数据：

- `buildings`：12 条
- `units`：240 条
- `residents`：360 条
- `fee_bills`：240 条
- `work_orders`：145 条
- `users`：3 条

总计 **1000 条**。

## 生成 SQL

```powershell
py .\scripts\generate_realistic_property_seed.py
```

默认输出到：

- `src/main/resources/db/realistic-property-seed.sql`

## 导入本地数据库

```powershell
$env:MYSQL_PWD='07966556167'
mysql -u root -h 127.0.0.1 -P 3306 -D archive_db --default-character-set=utf8mb4 < .\src\main\resources\db\realistic-property-seed.sql
```

## 新账号

- 管理员：`admin` / `Admin@2026`
- 物业管家：`manager` / `Manager@2026`
- 财务专员：`finance` / `Finance@2026`

> 数据均为合成示例，格式贴近中国物业管理场景，不对应真实个人。

