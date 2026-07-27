-- =============================================================================
-- Cloud 云盘 — 数据库初始化脚本
-- 5 张表覆盖用户、文件系统、分享、回收站、操作审计
-- 所有表使用 utf8mb4 字符集，支持中文等 4 字节字符（如 emoji）
--
-- 设计思路：
-- 1. 角色字段 role 存储 TINYINT（0/10/20/100）而非枚举字符串，空间更小且支持按数值比较大小
-- 2. 文件表采用 parent_id 树形结构（邻接表），简单直观，支持递归查询
-- 3. 分享表使用 share_token（UUID）而非自增 ID 做访问标识，防止链接被猜解
-- 4. 回收站独立表，删除时从 t_file 移入 t_recycle_bin，保留过期时间用于自动清理
-- 5. 操作日志表冗余所有字段（ip、user_agent、detail），不关联其他表，写入快且无外键约束
-- =============================================================================

CREATE DATABASE IF NOT EXISTS cloud DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cloud;

-- 用户表
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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文件表
CREATE TABLE IF NOT EXISTS t_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,              -- 父目录 ID，0 表示根目录
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,                        -- 完整路径（如 /documents/report.pdf）
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    extension VARCHAR(20) DEFAULT '',
    file_hash VARCHAR(64) DEFAULT '',                  -- 文件 SHA256，用于秒传校验
    is_directory TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录',
    object_name VARCHAR(255) DEFAULT '',               -- MinIO 中的对象路径
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已删除 1-正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_parent (user_id, parent_id, status) -- 按用户+目录列出文件的搜索加速
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 分享表
CREATE TABLE IF NOT EXISTS t_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    share_token VARCHAR(64) NOT NULL UNIQUE,           -- 分享链接唯一标识（UUID）
    access_password VARCHAR(64) DEFAULT '',            -- 提取密码（可选）
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-正常 1-已过期 2-已取消',
    expire_time DATETIME NOT NULL,
    max_download INT NOT NULL DEFAULT 0 COMMENT '0-不限',
    download_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_token (share_token),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表';

-- 回收站表
CREATE TABLE IF NOT EXISTS t_recycle_bin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(255) DEFAULT '',               -- MinIO 对象路径，到期删除
    parent_id BIGINT NOT NULL DEFAULT 0,               -- 恢复后放回原目录
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    deleted_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NOT NULL,                     -- 到期后物理删除
    INDEX idx_user (user_id),
    INDEX idx_expire (expire_time)                     -- 定时任务清理用
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT 0,
    operation VARCHAR(50) NOT NULL,                    -- 操作类型（LOGIN, UPLOAD...）
    target_type VARCHAR(50) NOT NULL,                  -- 操作对象类型（USER, FILE, SHARE）
    target_id BIGINT DEFAULT 0,                        -- 操作对象 ID
    detail VARCHAR(500) DEFAULT '',
    ip VARCHAR(50) DEFAULT '',
    user_agent VARCHAR(500) DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';