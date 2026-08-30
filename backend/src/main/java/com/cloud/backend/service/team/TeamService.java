package com.cloud.backend.service.team;

import com.cloud.backend.dto.team.TeamCreateRequest;
import com.cloud.backend.dto.team.TeamMemberResponse;
import com.cloud.backend.dto.team.TeamResponse;
import com.cloud.backend.dto.team.TeamUpdateRequest;
import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.enums.TeamMemberRole;

import java.util.List;

public interface TeamService {

    /* ==================== 团队 CRUD ==================== */

    Team create(Long userId, TeamCreateRequest request);

    TeamResponse update(Long teamId, Long userId, TeamUpdateRequest request);

    TeamResponse findResponse(Long teamId, Long userId);

    Team findById(Long id);

    List<Team> findAll();

    List<TeamResponse> listMyTeams(Long userId);

    /** 用户端解散（仅 OWNER） */
    void dissolve(Long teamId, Long operatorId);

    /* ==================== 成员管理 ==================== */

    List<TeamMemberResponse> listMembers(Long teamId, Long userId);

    /** 邀请成员（OWNER/ADMIN），成员数上限校验 */
    void invite(Long teamId, Long operatorId, List<Long> userIds);

    /** 移除成员（OWNER/ADMIN；不能移除 OWNER 与自身） */
    void removeMember(Long teamId, Long operatorId, Long targetUserId);

    /** 退出团队（OWNER 不能退出） */
    void leave(Long teamId, Long userId);

    /* ==================== 配额 ==================== */

    /** 团队剩余可用空间 = 配额 - 已用 */
    long getRemainingQuota(Long teamId);

    /** 原子调整团队已用空间（上传扣减为正、删除释放为负） */
    void changeUsedSpace(Long teamId, long delta);

    /** 配额预校验：超出抛 TEAM_QUOTA_EXCEEDED */
    void checkQuota(Long teamId, long size);

    /* ==================== 权限辅助 ==================== */

    /** 成员身份（null = 非成员） */
    TeamMember getMember(Long teamId, Long userId);

    /** 要求成员身份，否则抛 TEAM_NOT_MEMBER */
    TeamMember requireMember(Long teamId, Long userId);

    /** 要求 ADMIN+（含 OWNER） */
    TeamMember requireAdmin(Long teamId, Long userId);

    /** 要求 OWNER */
    TeamMember requireOwner(Long teamId, Long userId);

    /** 团队成员角色 */
    TeamMemberRole getMyRole(Long teamId, Long userId);

    /* ==================== 管理端（AdminTeamController 使用） ==================== */

    /** 成员数（管理端列表用） */
    long countMembers(Long teamId);

    /** 管理端查看成员（不要求调用者是该团队会员） */
    List<TeamMemberResponse> adminListMembers(Long teamId);

    /** 管理端调整配额（不能小于已用空间） */
    void adminUpdateQuota(Long teamId, Long quota);

    /** 管理端强制解散（不要求调用者是 OWNER） */
    void adminDissolve(Long teamId, Long operatorId);
}
