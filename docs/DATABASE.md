# Cloud 企业级云盘 — 数据库设计文档

> 版本: v0.1
> 更新日期: 2026-07-28
> 状态: Draft

---

## 1. 现有表

### 1.1 t_user — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(32) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt 加密密码 |
| email | VARCHAR(128) | UNIQUE, NOT NULL | 邮箱 |
| nickname | VARCHAR(50) | DEFAULT NULL | 昵称 |
| avatar | VARCHAR(255) | DEFAULT NULL | 头像 URL |
| role | TINYINT | NOT NULL, DEFAULT 0 | 0-USER 10-OPERATOR 20-ADMIN 100-SUPER_ADMIN |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-禁用 1-正常 |
| quota | BIGINT | NOT NULL | 总配额（字节） |
| used_space | BIGINT | NOT NULL, DEFAULT 0 | 已用空间 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- UNIQUE KEY uk_username (username)
- UNIQUE KEY uk_email (email)

### 1.2 t_file — 文件表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 文件 ID |
| user_id | BIGINT | NOT NULL | 所属用户 |
| parent_id | BIGINT | NOT NULL, DEFAULT 0 | 父目录 ID（0=根目录） |
| name | VARCHAR(255) | NOT NULL | 文件名/目录名 |
| size | BIGINT | DEFAULT 0 | 文件大小（目录=0） |
| mime_type | VARCHAR(128) | DEFAULT NULL | MIME 类型 |
| extension | VARCHAR(32) | DEFAULT NULL | 文件扩展名 |
| file_hash | VARCHAR(64) | DEFAULT NULL | SHA256，用于秒传 |
| object_name | VARCHAR(1024) | DEFAULT NULL | MinIO 对象路径 |
| is_directory | TINYINT(1) | NOT NULL, DEFAULT 0 | 是否目录 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-已删除 1-正常 |
| team_id | BIGINT | DEFAULT NULL | **新增**，所属团队（NULL=个人文件） |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- INDEX idx_user_parent (user_id, parent_id, status)
- INDEX idx_hash (file_hash, status)
- INDEX idx_team (team_id, parent_id, status) **新增**

### 1.3 t_share — 分享表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 分享 ID |
| user_id | BIGINT | NOT NULL | 创建者 |
| file_id | BIGINT | NOT NULL | 分享的文件 |
| share_token | VARCHAR(64) | UNIQUE, NOT NULL | UUID Token |
| access_password | VARCHAR(6) | DEFAULT NULL | 提取码 |
| expire_time | DATETIME | DEFAULT NULL | 过期时间（NULL=永久） |
| status | TINYINT | NOT NULL, DEFAULT 0 | 0-正常 1-已过期 2-已取消 |
| download_count | INT | NOT NULL, DEFAULT 0 | 下载次数 |
| team_id | BIGINT | DEFAULT NULL | **新增**，所属团队（NULL=个人文件分享） |
| created_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- UNIQUE KEY uk_token (share_token)
- INDEX idx_user (user_id)

### 1.4 t_recycle_bin — 回收站表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL | |
| file_id | BIGINT | NOT NULL | |
| original_name | VARCHAR(255) | NOT NULL | |
| object_name | VARCHAR(1024) | DEFAULT NULL | MinIO 对象路径 |
| parent_id | BIGINT | NOT NULL | 原父目录 |
| size | BIGINT | DEFAULT 0 | |
| mime_type | VARCHAR(128) | DEFAULT NULL | |
| deleted_time | DATETIME | NOT NULL | 删除时间 |
| expire_time | DATETIME | NOT NULL | 过期时间（30 天后） |

索引：
- PRIMARY KEY (id)
- INDEX idx_user (user_id)

### 1.5 t_operation_log — 操作日志表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | NOT NULL | |
| operation | VARCHAR(32) | NOT NULL | 操作类型（OperationType 枚举值） |
| target_type | VARCHAR(32) | DEFAULT NULL | 目标类型 |
| target_id | BIGINT | DEFAULT NULL | 目标 ID |
| detail | VARCHAR(512) | DEFAULT NULL | 操作详情 |
| ip | VARCHAR(128) | DEFAULT NULL | |
| user_agent | VARCHAR(512) | DEFAULT NULL | |
| created_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- INDEX idx_user (user_id)
- INDEX idx_operation (operation, created_at)

---

## 2. 新增表

### 2.1 t_team — 团队表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 团队 ID |
| name | VARCHAR(64) | NOT NULL | 团队名称 |
| owner_id | BIGINT | NOT NULL | 创建者用户 ID |
| avatar | VARCHAR(256) | DEFAULT NULL | 团队头像 |
| description | VARCHAR(512) | DEFAULT NULL | 团队描述 |
| quota | BIGINT | NOT NULL, DEFAULT 10GB | 团队总配额 |
| used_space | BIGINT | NOT NULL, DEFAULT 0 | 团队已用空间 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-解散 1-正常 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- INDEX idx_owner (owner_id)

### 2.2 t_team_member — 团队成员表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| team_id | BIGINT | NOT NULL | 团队 ID |
| user_id | BIGINT | NOT NULL | 用户 ID |
| role | TINYINT | NOT NULL, DEFAULT 0 | 0-成员 10-管理员 20-所有者 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-已退出 1-正常 |
| joined_at | DATETIME | NOT NULL | |

索引：
- PRIMARY KEY (id)
- UNIQUE KEY uk_team_user (team_id, user_id)
- INDEX idx_user (user_id)

---

## 3. 表关系

```
t_user (1) ──< t_file (N)         # 用户拥有文件
t_user (1) ──< t_share (N)        # 用户创建分享
t_user (1) ──< t_recycle_bin (N)  # 用户删除记录
t_user (1) ──< t_operation_log (N) # 用户操作日志
t_user (1) ──< t_team_member (N)   # 用户加入团队
t_team (1) ──< t_team_member (N)   # 团队包含成员
t_team (1) ──< t_file (N)         # 团队拥有文件（通过 team_id）
t_team (1) ──< t_share (N)        # 团队文件分享
```

---

## 4. 枚举值

### 4.1 用户

```
Role:         USER(0), OPERATOR(10), ADMIN(20), SUPER_ADMIN(100)
UserStatus:   DISABLED(0), NORMAL(1)
```

### 4.2 文件

```
FileStatus:   DELETED(0), NORMAL(1)
```

### 4.3 分享

```
ShareStatus:  NORMAL(0), EXPIRED(1), CANCELED(2)
```

### 4.4 团队

```
TeamMemberRole:  MEMBER(0), ADMIN(10), OWNER(20)
TeamStatus:      DISSOLVED(0), NORMAL(1)
```

### 4.5 操作日志

```
OperationType: LOGIN, REGISTER, LOGOUT,
               UPLOAD, DOWNLOAD, DELETE, RENAME, MOVE, COPY,
               SHARE, CANCEL_SHARE,
               TEAM_CREATE, TEAM_DISSOLVE, TEAM_INVITE, TEAM_REMOVE, TEAM_LEAVE,
               ADMIN_OPERATION
TargetType:    USER, FILE, SHARE, TEAM
```

---

## 5. 分表策略说明

- MVP 阶段所有表在同一 MySQL 实例，不拆分
- t_operation_log 写入频繁但数据量可控（家庭使用），暂不分表
- 后续用户量增长时可按 user_id 分表或迁移至时序数据库

---

## 6. 定时任务清单

| 任务 | 频率 | 说明 |
|------|------|------|
| UploadCleanupTask | 每小时 | 清理 Redis 中超 2 小时未更新的 uploadId、对应的 MinIO 临时分片 |
| RecycleBinCleanupTask | 每天 | 扫描 t_recycle_bin 中 expire_time < now() 的记录，物理删除 MinIO 对象后删除回收站记录 |
