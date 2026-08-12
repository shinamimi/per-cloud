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
 *
 * 修改指引：
 * - 【习惯】修改 id / name / avatar / description → Long id（t_team.id 主键）/ String name（name 团队名）/ String avatar
 *                            （avatar 头像）/ String description（description 描述）；仅团队展示与创建，无业务联动
 * - 【统一】修改 ownerId          → Long ownerId；对应 t_team.owner_id，创建者（队长），改它需同步成员角色
 *                            （t_team_member 中 role=OWNER）；
 *                            改后需同步 t_team_member 中 role=OWNER 的成员记录
 * - 【统一】修改 status           → TeamStatus status；对应 t_team.status（TINYINT），NORMAL=1/DISSOLVED=0（见 enums/TeamStatus.java，
 *                            按 ordinal 存库）；解散时成员记录 status 一并置 0，改枚举见 TeamStatus 修改指引；
 *                            改后需同步 DB 存量数据与解散时联动 t_team_member.status 置 0 的逻辑
 * - 【统一】修改 quota            → Long quota；对应 t_team.quota（BIGINT，单位字节），团队总配额（基础 + 管理端赠送），
 *                            影响 TeamServiceImpl 的剩余配额计算与上传拦截；
 *                            改后需同步 TeamServiceImpl 剩余配额计算与上传拦截（单位字节口径一致）
 * - 【统一】修改 usedSpace        → Long usedSpace；对应 t_team.used_space（BIGINT，单位字节），团队已用空间，上传/删除时更新；
 *                            改后需同步上传/删除时更新 usedSpace 的逻辑（单位字节口径一致）
 * - 【习惯】修改 createdAt / updatedAt → LocalDateTime；自动维护，无业务联动
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
