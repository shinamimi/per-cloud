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
