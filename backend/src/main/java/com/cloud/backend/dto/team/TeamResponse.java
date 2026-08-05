package com.cloud.backend.dto.team;

import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.TeamMemberRole;
import lombok.Data;

/**
 * 团队响应 —— 详情/列表共用。
 * myRole：当前用户在该团队中的角色（MEMBER/ADMIN/OWNER），便于前端权限控制。
 *
 * 修改指引：
 * - 【习惯】修改 id              → Long id；团队 id
 * - 【习惯】修改 name / avatar / description → 团队基本资料，仅展示用
 * - 【习惯】修改 ownerId / ownerName → 团队所有者 id 与昵称；前端据此展示所有者标识
 * - 【习惯】修改 quota / usedSpace → Long quota / Long usedSpace；团队配额与已用空间，单位：字节（非 KB/MB），
 *                         前端展示需换算并计算剩余空间；usedSpace 超 quota 时前端需展示超量状态
 * - 【习惯】修改 memberCount     → Long memberCount；成员数，前端列表/详情展示
 * - 【习惯】修改 myRole          → TeamMemberRole myRole；当前用户在该团队的角色（enums/TeamMemberRole.java：
 *                         MEMBER=0 / ADMIN=10 / OWNER=20），前端据此控制成员管理/解散/退出按钮显隐
 * - 【习惯】修改 createdAt       → String createdAt；创建时间（字符串类型，非时间对象）
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
