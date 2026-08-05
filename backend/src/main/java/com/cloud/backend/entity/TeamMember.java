package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队成员实体 —— 对应数据库 t_team_member 表，描述用户与团队的成员关系。
 *
 * 设计思路：
 * - role 存自定义 value（0/10/20）而非 ordinal，注册了专用类型处理器
 * - status 用 1/0 表达成员关系有效性：1=正常，0=退出/被移除（保留记录便于追溯）
 *
 * 修改指引：
 * - 【习惯】修改 id / joinedAt    → Long id（t_team_member.id 主键）/ LocalDateTime joinedAt（joined_at 加入时间）；仅记录，无业务联动
 * - 【习惯】修改 teamId / userId  → Long teamId（t_team_member.team_id）/ Long userId（user_id）；唯一索引
 *                            uk_team_user(team_id, user_id) 约束成员唯一，改字段名需同步 DDL
 * - 【习惯】修改 role             → TeamMemberRole role；对应 t_team_member.role（TINYINT），MEMBER=0/ADMIN=10/OWNER=20
 *                            （见 enums/TeamMemberRole.java，由 TeamMemberRoleTypeHandler 按 value 存取，非 ordinal）；
 *                            权限判断（getValue() >= ADMIN 等）同步受影响，改枚举见 TeamMemberRole 修改指引
 * - 【习惯】修改 status           → Integer status；对应 t_team_member.status（TINYINT），1=正常 0=退出/被移除，
 *                            团队解散（TeamStatus.DISSOLVED）或成员退出时置 0（记录保留追溯），与 Team 解散状态联动
 */
@Data
public class TeamMember {

    /** 成员关系记录 ID */
    private Long id;
    /** 团队 ID */
    private Long teamId;
    /** 用户 ID */
    private Long userId;
    /** 成员角色（MEMBER=0 / ADMIN=10 / OWNER=20） */
    private TeamMemberRole role;
    /** 成员关系状态（1=正常，0=已退出/被移除） */
    private Integer status;
    /** 加入时间 */
    private LocalDateTime joinedAt;
}
