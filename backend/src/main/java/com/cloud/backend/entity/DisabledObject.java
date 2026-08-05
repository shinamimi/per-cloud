package com.cloud.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对象级禁用记录 —— 对应数据库 t_disabled_object 表。
 *
 * 设计思路（禁用粒度）：
 * 禁用落在「内容对象（file_hash）」维度而非文件记录维度：
 * - scope=GLOBAL：该 hash 全站禁用，任何用户引用/上传该内容都被拦截
 * - scope=USER：用户×hash 禁用，只影响该用户，其他用户不受影响
 * 秒传/重新上传命中被禁对象 → 拦截提示"上传违规文件"。
 *
 * 修改指引：
 * - 【习惯】修改 fileHash          → String fileHash；对应 t_disabled_object.file_hash（内容 SHA256，与 t_file.file_hash 同源），
 *                            是禁用命中的核心维度；改字段名/长度需同步迁移脚本 DDL，并保持与上传/秒传拦截逻辑一致
 * - 【习惯】修改 scope             → Integer scope；对应 t_disabled_object.scope（TINYINT），取值 1=GLOBAL / 2=USER
 *                            （见 enums/DisableScope.java）；改取值或新增粒度会影响上传拦截时的命中判定
 * - 【习惯】修改 userId            → Long userId；对应 t_disabled_object.user_id，scope=1 时恒为 0、scope=2 时为被禁目标用户；
 *                            改列名需同步唯一索引 uk_hash_scope_user 的 DDL
 * - 【习惯】修改 createdBy / reason → Long createdBy（t_disabled_object.created_by 操作管理员 id）/ String reason（reason 禁用原因）；
 *                            仅审计与展示，不影响禁用命中逻辑
 * - 【习惯】修改 id / createdAt    → Long id（t_disabled_object.id 主键）/ LocalDateTime createdAt（created_at）；仅自增与记录时间，无业务联动
 * - 【习惯】修改唯一约束          → uk_hash_scope_user(file_hash, scope, user_id) 决定禁用记录唯一性；调整禁用粒度或新增维度需同步 DDL
 */
@Data
public class DisabledObject {

    private Long id;
    private String fileHash;
    private Integer scope;
    private Long userId;
    private Long createdBy;
    private String reason;
    private LocalDateTime createdAt;
}
