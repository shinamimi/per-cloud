package com.cloud.backend.service.team;

import com.cloud.backend.dto.team.TeamCreateRequest;
import com.cloud.backend.dto.team.TeamMemberResponse;
import com.cloud.backend.dto.team.TeamResponse;
import com.cloud.backend.dto.team.TeamUpdateRequest;
import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.TeamMember;
import com.cloud.backend.enums.TeamMemberRole;

import java.util.List;

/**
 * 团队服务 —— 团队 CRUD / 成员管理 / 配额。
 * 权限矩阵：OWNER 最高；ADMIN 可成员管理；MEMBER 只读+退出。
 * 配额：独立配额（t_team.quota + used_space），成员上传占团队配额。
 *
 * 修改指引：
 * - 【习惯】想改"创建团队（OWNER 自动成为成员/团队数上限/默认配额）" → create() 对应 TeamServiceImpl.create()
 *   （@Transactional；team.max-per-user 与 team.default-quota 配置；写 TEAM_CREATE 日志）；改动影响团队准入与配额
 * - 【习惯】想改"更新/查询/我的团队" → update()/findResponse()/listMyTeams()（ADMIN 可改；带成员数与本人角色）；
 *   改动影响团队信息展示与权限
 * - 【习惯】想改"解散（用户端仅 OWNER / 管理端强制）" → dissolve()/adminDissolve()（@Transactional；成员 status 置 0
 *   + 写 TEAM_DISSOLVE 日志）；改动影响团队生命周期与残留成员
 * - 【习惯】想改"邀请/移除/退出" → invite()/removeMember()/leave()（@Transactional；成员数上限、ADMIN 不能移除其他
 *   ADMIN、OWNER 不能退出；逐项写日志）；改动影响团队人数与权限矩阵
 * - 【习惯】想改"配额（剩余空间/原子调整/预校验）" → getRemainingQuota()/changeUsedSpace()（Mapper 原子 SQL 自增自减，
 *   勿改读改写）/checkQuota()（超出抛 TEAM_QUOTA_EXCEEDED）；改动影响团队配额一致性与上传准入
 * - 【习惯】想改"权限辅助（requireMember/requireAdmin/requireOwner）" → 对应方法；改动影响全部团队接口的准入边界
 * - 【习惯】想改"管理端调整配额（不能小于已用空间）" → adminUpdateQuota()；改动影响团队扩容/缩容
 * - 【习惯】新增方法 → 需同步实现类 TeamServiceImpl 与 TeamController/AdminTeamController 调用方
 */
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
