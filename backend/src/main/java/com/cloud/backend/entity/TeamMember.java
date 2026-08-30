package com.cloud.backend.entity;

import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;
import java.time.LocalDateTime;

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
