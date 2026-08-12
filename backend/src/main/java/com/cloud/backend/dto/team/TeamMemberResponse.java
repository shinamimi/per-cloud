package com.cloud.backend.dto.team;

import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;

/**
 * 团队成员项
 *
 * 修改指引：
 * - 【统一】修改 userId          → Long userId；成员用户 id，移除成员接口 DELETE /api/teams/{id}/members/{userId} 使用；改名需同步前端移除成员操作与 TeamMemberService
 * - 【习惯】修改 username / nickname / avatar → 成员基本资料，仅展示用
 * - 【统一】修改 role            → TeamMemberRole role；自定义枚举（enums/TeamMemberRole.java）：MEMBER=0 / ADMIN=10 / OWNER=20，
 *                         前端据此展示角色与操作权限（OWNER 不可被移除）；改后需同步 enums/TeamMemberRole.java 与权限判定逻辑
 * - 【习惯】修改 joinedAt        → String joinedAt；加入时间（LocalDateTime.toString 转字符串），
 *                         前端排序/格式化时注意是字符串类型而非时间对象
 */
@Data
public class TeamMemberResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private TeamMemberRole role;
    private String joinedAt;

    public static TeamMemberResponse from(TeamMember member, User user) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setUserId(member.getUserId());
        response.setRole(member.getRole());
        response.setJoinedAt(member.getJoinedAt() == null ? null : member.getJoinedAt().toString());
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }
}
