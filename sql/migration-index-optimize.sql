-- =============================================================================
-- Cloud 云盘 — 数据库索引优化
-- 执行方式：mysql -uroot -p cloud < sql/migration-index-optimize.sql
-- =============================================================================

-- 1. 删除冗余索引（t_share.idx_token 与 share_token UNIQUE 重复）
DROP INDEX IF EXISTS idx_token ON t_share;

-- 2. 添加缺失索引

-- t_file: 按类型筛选文件（图片/文档/视频等）
ALTER TABLE t_file ADD INDEX idx_user_type_status (user_id, type, status);

-- t_file: 秒传校验 + 用户文件去重
ALTER TABLE t_file ADD INDEX idx_user_file_hash (user_id, file_hash);

-- t_share: "我的分享"列表（按状态筛选）
ALTER TABLE t_share ADD INDEX idx_user_status (user_id, status);

-- t_recycle_bin: 用户回收站列表（区分用户删除/管理员删除）
ALTER TABLE t_recycle_bin ADD INDEX idx_user_deleted_by (user_id, deleted_by);

-- t_operation_log: 审计日志复合查询
ALTER TABLE t_operation_log ADD INDEX idx_user_operation_created (user_id, operation, created_at);

-- t_file_hash: GC 扫描零引用记录
ALTER TABLE t_file_hash ADD INDEX idx_ref_count (ref_count);
