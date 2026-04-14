# spring-boot-management-system

## 项目简介

本项目是一个基于 Spring Boot 的物业管理系统，支持住户、房屋、账单、通知等多种管理功能，适用于小区、写字楼等场景。前端采用 Thymeleaf 模板，后端基于 Java Spring Boot 框架，支持多种数据库。

## 主要功能
- 住户档案管理（业主/租客信息录入、编辑、删除）
- 房屋信息管理（房屋档案、状态同步、业主/租客关联）
- 账单与缴费管理（账单生成、缴费记录、历史查询）
- 通知公告管理（系统公告、住户通知推送）
- 权限与账号管理（多角色支持、权限分配、住户/管理员账号）
- 数据统计与报表（住户统计、房屋状态、缴费报表）
- 操作日志与安全审计

## 目录结构
```
src/
  main/
    java/org/example/                 # 后端 Java 代码
      propertyms/                     # 物业管理系统核心模块
        controller/                   # 控制器（Web 层）
        service/                      # 业务逻辑层
        repository/                   # 数据访问层
        model/                        # 实体类与 DTO
        config/                       # 配置类
        util/                         # 工具类
    resources/
      static/                         # 前端静态资源（CSS/JS/图片）
        css/                          # 样式文件
        js/                           # 脚本文件
        images/                       # 图片资源
        icons/                        # 图标资源
      templates/                      # Thymeleaf 模板页面
        account/                      # 账号相关页面
        admin/                        # 管理后台页面
        auth/                         # 登录/注册/认证页面
        error/                        # 错误页面
        fragments/                    # 公共片段
      application.properties          # 主配置文件
      messages.properties             # 国际化资源
  test/
    java/org/example/                 # 单元测试代码
    resources/                        # 测试资源文件
scripts/                              # 数据库脚本、测试脚本
  *.sql                               # 数据库结构与初始化脚本
  *.py                                # 数据生成脚本
  jmeter/                             # 性能测试脚本
```

## 快速开始
1. 克隆项目：
   ```bash
   git clone <仓库地址>
   ```
2. 配置数据库连接（修改 `src/main/resources/application.properties`）
3. 初始化数据库（可选，执行 `scripts/` 下 SQL 脚本）
4. 构建并运行：
   ```bash
   ./mvnw spring-boot:run
   ```
5. 访问系统：
   浏览器打开 http://localhost:8080

## 依赖环境
- JDK 17 或以上
- Maven 3.6+
- MySQL/PostgreSQL/其他兼容数据库

## 主要模块说明
### 1. 住户管理
- 住户信息录入、编辑、删除
- 住户类型（业主/租客）区分
- 入住/迁出日期管理
- 住户与房屋的关联关系

### 2. 房屋管理
- 房屋档案维护（单元号、楼栋、面积等）
- 房屋状态同步（在住/空置/出租）
- 业主与租客同步

### 3. 账单与缴费
- 账单自动生成与手动录入
- 缴费记录管理
- 支持多种费用类型
- 历史账单查询

### 4. 通知公告
- 系统公告发布
- 住户通知推送
- 通知已读/未读状态

### 5. 权限与账号
- 支持多角色（管理员、住户等）
- 账号注册、登录、找回密码
- 权限分配与管理

### 6. 数据统计与报表
- 住户数量、房屋状态统计
- 缴费情况报表
- 导出功能

### 7. 日志与安全
- 操作日志记录
- 关键操作二次确认
- 数据安全与权限校验

## 常用命令
- 启动开发环境：`./mvnw spring-boot:run`
- 打包：`./mvnw clean package`
- 运行测试：`./mvnw test`

## 贡献指南
1. Fork 本仓库并新建分支
2. 提交代码前请确保通过所有测试
3. 提交 PR 前请详细描述变更内容
4. 欢迎 issue 反馈与建议

## License
MIT
