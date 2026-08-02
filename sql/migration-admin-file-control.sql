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
