package com.cloud.backend.dto.team;

import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;

/** 团队成员项 */
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
