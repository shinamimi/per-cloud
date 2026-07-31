-- =============================================================================
-- Cloud 云盘 — 文件模块升级脚本
-- 1. t_file 统一表模型：type 区分 FILE/DIRECTORY、category 文件分类、team_id 归属
-- 2. t_file_hash 秒传索引表（SHA256 → 共享对象 + 引用计数）
-- 3. t_setting 系统设置表（上传限制等管理员可配置项）
-- 4. t_recycle_bin 扩展：file_hash（物理清理时释放引用）、type、team_id
-- 用法：mysql -uroot -p <库名> < migration-file-module.sql
-- =============================================================================

-- 1. t_file 统一表模型
ALTER TABLE t_file
    ADD COLUMN type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录' AFTER is_directory,
    ADD COLUMN category TINYINT NOT NULL DEFAULT 5 COMMENT '0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他' AFTER type;

-- team_id 列已存在（早期预留），规范化为 NOT NULL DEFAULT 0
UPDATE t_file SET team_id = 0 WHERE team_id IS NULL;
ALTER TABLE t_file
    MODIFY COLUMN team_id BIGINT NOT NULL DEFAULT 0 COMMENT '0-个人空间，>0-团队空间' AFTER user_id;

-- 存量数据同步：is_directory → type
UPDATE t_file SET type = is_directory WHERE type = 0;

-- 唯一索引防同名并发竞态（同一空间同目录下 name 唯一；业务层重名自动加后缀）
ALTER TABLE t_file ADD UNIQUE KEY uk_user_parent_name (user_id, parent_id, name, team_id);

-- 2. 秒传索引表
CREATE TABLE IF NOT EXISTS t_file_hash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL COMMENT '文件 SHA256',
    object_name VARCHAR(255) NOT NULL COMMENT '共享对象路径',
    size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(100) DEFAULT '',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '全局引用计数，归零物理删除对象',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒传索引表';

-- 3. 系统设置表
CREATE TABLE IF NOT EXISTS t_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(64) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL DEFAULT '',
    description VARCHAR(255) DEFAULT '',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- 4. 回收站表扩展
ALTER TABLE t_recycle_bin
    ADD COLUMN file_hash VARCHAR(64) DEFAULT '' COMMENT '文件 SHA256，物理清理时释放引用' AFTER object_name,
    ADD COLUMN type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文件 1-目录' AFTER file_hash,
    ADD COLUMN team_id BIGINT NOT NULL DEFAULT 0 COMMENT '0-个人空间' AFTER type;
