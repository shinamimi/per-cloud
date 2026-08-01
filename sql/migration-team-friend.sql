-- =============================================================================
-- Cloud 云盘 — 团队模块 + 好友系统升级脚本
-- 1. t_team 补配额字段（实体 Team 已有 quota/usedSpace，数据库缺失）
-- 2. t_friend_request / t_friendship 好友关系表（双向确认，ADR-010）
-- 用法：mysql -uroot -p <库名> < migration-team-friend.sql
-- =============================================================================

-- 1. 团队配额：独立配额（t_team.quota，管理员后台分配）+ 已用空间
ALTER TABLE t_team
    ADD COLUMN quota BIGINT NOT NULL DEFAULT 10737418240 COMMENT '团队配额（字节），默认10GB' AFTER status,
    ADD COLUMN used_space BIGINT NOT NULL DEFAULT 0 COMMENT '已用空间（字节）' AFTER quota;

-- 2. 好友请求表（双向确认：A 发请求 → B 接受/拒绝）
CREATE TABLE IF NOT EXISTS t_friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL COMMENT '发起方',
    to_user_id BIGINT NOT NULL COMMENT '接收方',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待接受 ACCEPTED-已接受 REJECTED-已拒绝',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_from_to (from_user_id, to_user_id),
    KEY idx_to_status (to_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';

-- 3. 好友关系表（成对存储，user_a < user_b，查询便捷）
CREATE TABLE IF NOT EXISTS t_friendship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a_id BIGINT NOT NULL COMMENT '较小用户ID',
    user_b_id BIGINT NOT NULL COMMENT '较大用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (user_a_id, user_b_id),
    KEY idx_a (user_a_id),
    KEY idx_b (user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';
