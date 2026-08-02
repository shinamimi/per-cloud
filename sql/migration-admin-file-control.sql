-- ============================================================
-- 迁移：管理后台文件管控（docs/adr/012-admin-file-control.md）
-- 双回收站：t_recycle_bin 增加删除来源字段
--   deleted_by = 0 用户自删（私有回收站，仅用户可见）
--   deleted_by = 1 管理员删（全局回收站，仅管理员可见）
-- 老数据默认 0（用户自删），语义不变，安全。
-- t_file.status 新增语义：2=DISABLED（禁用，用户可见但不可下载/预览）
--   无需改表（status 注释含义扩展）
-- ============================================================

ALTER TABLE t_recycle_bin
    ADD COLUMN deleted_by TINYINT NOT NULL DEFAULT 0 COMMENT '0-用户删除(私有回收站) 1-管理员删除(全局回收站)' AFTER team_id;

ALTER TABLE t_recycle_bin ADD INDEX idx_deleted_by (deleted_by, team_id);

-- =====================================================================
-- 2026-08-02 追加：对象级禁用（docs/admin-file-management.md 5.1 禁用粒度）
-- 全站禁（scope=1，按内容 hash 禁用）+ 仅用户（scope=2，用户×内容）
-- 秒传/重新上传命中被禁对象 → 拦截"上传违规文件"
-- =====================================================================
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
