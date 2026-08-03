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
