package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队实体 —— 对应数据库 t_team 表。
 *
 * 设计思路：
 * - status 用 TeamStatus 枚举（NORMAL=1 / DISSOLVED=0），存储 TINYINT（EnumOrdinalTypeHandler）
 * - quota / usedSpace 均为字节单位，quota 为团队总配额（基础 + 管理端赠送）
 */
@Data
public class Team {

    /** 团队 ID */
    private Long id;
    /** 团队名称 */
    private String name;
    /** 创建者（队长）用户 ID */
    private Long ownerId;
    /** 团队头像地址 */
    private String avatar;
    /** 团队描述 */
    private String description;
    /** 团队状态（NORMAL=正常 / DISSOLVED=已解散） */
    private TeamStatus status;
    /** 团队总配额（单位：字节） */
    private Long quota;
    /** 已用空间（单位：字节） */
    private Long usedSpace;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最近更新时间 */
    private LocalDateTime updatedAt;
}
