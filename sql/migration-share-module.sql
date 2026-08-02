-- 分享模块扩展（docs/share-module.md M4）：
-- 1. t_share 加下载策略/转存开关/目录分享标识；expire_time 允许 NULL（永久分享）
-- 2. t_share_file 分享快照表（文件夹分享锁定创建时的目录树）
-- 执行方式：docker exec -i cloud-mysql mysql -uroot -proot cloud_drive < 本文件

-- t_share 扩展
ALTER TABLE t_share
    ADD COLUMN allow_download TINYINT NOT NULL DEFAULT 1 COMMENT '1=允许下载 0=禁止下载（只能在线预览）' AFTER download_count,
    ADD COLUMN allow_save TINYINT NOT NULL DEFAULT 1 COMMENT '1=允许转存 0=禁止转存' AFTER allow_download,
    ADD COLUMN is_dir TINYINT NOT NULL DEFAULT 0 COMMENT '1=目录分享（快照锁定）0=单文件分享' AFTER file_id;

ALTER TABLE t_share MODIFY COLUMN expire_time DATETIME NULL COMMENT '过期时间，NULL=永久';

-- 分享快照表：分享创建时锁定的文件树（文件被改名/删除/新增均不影响已分享内容）
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
