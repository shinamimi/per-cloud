-- =============================================================================
-- Cloud 云盘 — 生产环境全新初始化脚本（全量建库建表）
-- 合并 schema.sql + 全部 migration-*.sql 的最终结构，可用于全新服务器一次导入。
-- 用法：mysql -uroot -p < init-full.sql （或经 docker-entrypoint-initdb.d 自动执行）
--
-- 说明：
-- 1. 此脚本是「最终结构」快照，直接 CREATE TABLE IF NOT EXISTS，不在已有库上重复 ALTER
-- 2. t_team / t_team_member 在旧 migration 中只有补字段 ALTER，缺失建表，本脚本补齐
-- 3. charset 统一 utf8mb4；枚举字段存 TINYINT（自定义 value，禁用 ordinal 定义在 DDL 中注释）
-- 4. 与后端 entity / mapper 严格对齐（见 docs/DATABASE.md）
-- =============================================================================

CREATE DATABASE IF NOT EXISTS cloud DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cloud;

-- ---------------------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,                   -- BCrypt 加密存储
    email VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(50) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0-USER 10-OPERATOR 20-ADMIN 100-SUPER_ADMIN',
    quota BIGINT NOT NULL DEFAULT 10737418210 COMMENT '默认10GB',
    used_space BIGINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    is_vip TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通用户 1-VIP',
    admin_bonus_quota BIGINT NOT NULL DEFAULT 0 COMMENT '管理员赠送容量(字节)',
    reward_quota BIGINT NOT NULL DEFAULT 0 COMMENT '奖励容量(字节)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------------------------
-- 文件表（统一表模型：文件/目录/团队空间）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL DEFAULT 0 COMMENT '0-个人空间，>0-团队空间',
    parent_id BIGINT NOT NULL DEFAULT 0,              -- 父目录 ID，0 表示根目录
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,                        -- 完整路径（如 /documents/report.pdf）
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    extension VARCHAR(20) DEFAULT '',
    file_hash VARCHAR(64) DEFAULT '',                  -- 文件 SHA256，用于秒传校验
    is_directory TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录（兼容旧字段）',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录',
    category TINYINT NOT NULL DEFAULT 5 COMMENT '0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他',
    object_name VARCHAR(255) DEFAULT '',               -- MinIO 中的对象路径
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已删除 1-正常 2-禁用(管理员)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_parent (user_id, parent_id, status),
    UNIQUE KEY uk_user_parent_name (user_id, parent_id, name, team_id),
    INDEX idx_user_type_status (user_id, type, status),
    INDEX idx_user_file_hash (user_id, file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ---------------------------------------------------------------------------
-- 秒传索引表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_file_hash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL COMMENT '文件 SHA256',
    object_name VARCHAR(255) NOT NULL COMMENT '共享对象路径',
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '全局引用计数，归零物理删除对象',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hash (file_hash),
    INDEX idx_ref_count (ref_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒传索引表';

-- ---------------------------------------------------------------------------
-- 系统设置表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(64) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL DEFAULT '',
    description VARCHAR(255) DEFAULT '',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- ---------------------------------------------------------------------------
-- 分享表（支持目录分享/下载策略/转存开关）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    is_dir TINYINT NOT NULL DEFAULT 0 COMMENT '1=目录分享（快照锁定）0=单文件分享',
    share_token VARCHAR(64) NOT NULL UNIQUE,           -- 分享链接唯一标识（UUID）
    access_password VARCHAR(64) DEFAULT '',            -- 提取密码（可选）
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常 1-已过期 2-已取消',
    expire_time DATETIME NULL COMMENT '过期时间，NULL=永久',
    max_download INT NOT NULL DEFAULT 0 COMMENT '0-不限',
    download_count INT NOT NULL DEFAULT 0,
    allow_download TINYINT NOT NULL DEFAULT 1 COMMENT '1=允许下载 0=禁止下载（只能在线预览）',
    allow_save TINYINT NOT NULL DEFAULT 1 COMMENT '1=允许转存 0=禁止转存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表';

-- ---------------------------------------------------------------------------
-- 分享快照表（文件夹分享锁定创建时的目录树）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_share_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    share_id BIGINT NOT NULL COMMENT '分享 id',
    file_id BIGINT NOT NULL COMMENT '原文件 id（下载/预览时校验原文件状态）',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '快照树父节点 id（0=根；指向本表 id）',
    name VARCHAR(255) NOT NULL,
    is_dir TINYINT NOT NULL DEFAULT 0,
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128) DEFAULT '',
    extension VARCHAR(64) DEFAULT '',
    file_hash VARCHAR(64) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_share (share_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享快照表';

-- ---------------------------------------------------------------------------
-- 团队表（含配额/已用空间，旧 migration 缺失建表）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    owner_id BIGINT NOT NULL COMMENT '创建者用户 ID',
    avatar VARCHAR(256) DEFAULT '' COMMENT '团队头像',
    description VARCHAR(512) DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-解散 1-正常',
    quota BIGINT NOT NULL DEFAULT 10737418240 COMMENT '团队配额（字节），默认10GB',
    used_space BIGINT NOT NULL DEFAULT 0 COMMENT '已用空间（字节）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队表';

-- ---------------------------------------------------------------------------
-- 团队成员表（role 存自定义 value：MEMBER=0  ADMIN=10  OWNER=20）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_team_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0-MEMBER 10-ADMIN 20-OWNER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 0-已退出/被移除',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表';

-- ---------------------------------------------------------------------------
-- 好友请求表（双向确认：A 发请求 → B 接受/拒绝）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL COMMENT '发起方',
    to_user_id BIGINT NOT NULL COMMENT '接收方',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待接受 ACCEPTED-已接受 REJECTED-已拒绝',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_from_to (from_user_id, to_user_id),
    KEY idx_to_status (to_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';

-- ---------------------------------------------------------------------------
-- 好友关系表（成对存储，user_a < user_b，查询便捷）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_friendship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a_id BIGINT NOT NULL COMMENT '较小用户ID',
    user_b_id BIGINT NOT NULL COMMENT '较大用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (user_a_id, user_b_id),
    KEY idx_a (user_a_id),
    KEY idx_b (user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- ---------------------------------------------------------------------------
-- 回收站表（双回收站：用户自删 deleted_by=0 / 管理员删 deleted_by=1）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_recycle_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(255) DEFAULT '',               -- MinIO 对象路径，到期删除
    file_hash VARCHAR(64) DEFAULT '' COMMENT '文件 SHA256，物理清理时释放引用',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录',
    team_id BIGINT NOT NULL DEFAULT 0 COMMENT '0-个人空间，>0-团队空间',
    deleted_by TINYINT NOT NULL DEFAULT 0 COMMENT '0-用户删除(私有回收站) 1-管理员删除(全局回收站)',
    parent_id BIGINT NOT NULL DEFAULT 0,               -- 恢复后放回原目录
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    deleted_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NOT NULL,                     -- 到期后物理删除
    INDEX idx_user (user_id),
    INDEX idx_expire (expire_time),
    INDEX idx_deleted_by (deleted_by, team_id),
    INDEX idx_user_deleted_by (user_id, deleted_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站表';

-- ---------------------------------------------------------------------------
-- 操作日志表（冗余字段，不关联其他表，写入快）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT 0,
    operation VARCHAR(50) NOT NULL COMMENT '操作类型（LOGIN, UPLOAD_FILE...）',
    target_type VARCHAR(50) NOT NULL COMMENT '操作对象类型（USER, FILE, SHARE, TEAM）',
    target_id BIGINT DEFAULT 0,
    detail VARCHAR(500) DEFAULT '',
    ip VARCHAR(50) DEFAULT '',
    user_agent VARCHAR(500) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at),
    INDEX idx_user_operation_created (user_id, operation, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ---------------------------------------------------------------------------
-- 对象级禁用记录（按内容 hash 维度：全站禁 GLOBAL=1 / 仅用户 USER=2）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_disabled_object (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_hash VARCHAR(64) NOT NULL COMMENT '内容 hash（t_file.file_hash）',
    scope TINYINT NOT NULL DEFAULT 1 COMMENT '1=全站禁(GLOBAL) 2=仅用户(USER)',
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT 'scope=2 时的目标用户，scope=1 恒为 0',
    created_by BIGINT NOT NULL COMMENT '操作管理员 id',
    reason VARCHAR(255) DEFAULT NULL COMMENT '禁用原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hash_scope_user (file_hash, scope, user_id),
    KEY idx_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象级禁用记录（内容 hash 维度）';