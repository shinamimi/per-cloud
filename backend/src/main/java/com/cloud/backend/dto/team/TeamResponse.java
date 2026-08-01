package com.cloud.backend.dto.team;

import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;

/**
 * 团队响应 —— 详情/列表共用。
 * myRole：当前用户在该团队中的角色（MEMBER/ADMIN/OWNER），便于前端权限控制。
 */
@Data
public class TeamResponse {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Long quota;
    private Long usedSpace;
    private Long memberCount;
    private TeamMemberRole myRole;
    private String createdAt;

    public static TeamResponse from(Team team, String ownerName, Long memberCount, TeamMemberRole myRole) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setAvatar(team.getAvatar());
        response.setDescription(team.getDescription());
        response.setOwnerId(team.getOwnerId());
        response.setOwnerName(ownerName);
        response.setQuota(team.getQuota());
        response.setUsedSpace(team.getUsedSpace());
        response.setMemberCount(memberCount);
        response.setMyRole(myRole);
        response.setCreatedAt(team.getCreatedAt() == null ? null : team.getCreatedAt().toString());
        return response;
    }
}
