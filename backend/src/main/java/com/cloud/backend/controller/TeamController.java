package com.cloud.backend.controller;

import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.team.TeamCreateRequest;
import com.cloud.backend.dto.team.TeamInviteRequest;
import com.cloud.backend.dto.team.TeamMemberResponse;
import com.cloud.backend.dto.team.TeamResponse;
import com.cloud.backend.dto.team.TeamUpdateRequest;
import com.cloud.backend.service.team.TeamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 团队控制器（用户端接口）。
 * 权限矩阵：创建=登录用户；解散=OWNER；成员管理=ADMIN+；退出=MEMBER+（OWNER 除外）。
 *
 * 修改指引：
 * - 【习惯】创建团队           → POST /api/teams，调 teamService.create / findResponse；登录用户即可创建
 * - 【习惯】我的团队列表 / 详情  → GET /api/teams、GET /api/teams/{id}，调 teamService.listMyTeams / findResponse
 * - 【习惯】更新团队信息       → PUT /api/teams/{id}，调 teamService.update；权限 OWNER/ADMIN（服务内校验）
 * - 【习惯】解散团队           → DELETE /api/teams/{id}，调 teamService.dissolve；仅 OWNER
 * - 【习惯】邀请成员           → POST /api/teams/{id}/members，调 teamService.invite；OWNER/ADMIN，可从好友列表选人
 * - 【习惯】成员列表           → GET /api/teams/{id}/members，调 teamService.listMembers
 * - 【习惯】移除成员           → DELETE /api/teams/{id}/members/{userId}，调 teamService.removeMember；OWNER/ADMIN，
 *                        不能移除 OWNER 与自身
 * - 【习惯】退出团队           → POST /api/teams/{id}/leave，调 teamService.leave；OWNER 不能退出
 * - 【习惯】新增/修改接口       → 权限矩阵（OWNER/ADMIN/MEMBER+）在 TeamService 内校验，改权限需同步服务层与前端；
 *                        需登录，若为公开接口须在 SecurityConfig 放行
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /** 创建团队 */
    @PostMapping
    public Result<TeamResponse> create(@Valid @RequestBody TeamCreateRequest request) {
        Long userId = AuthorizationPolicy.getCurrentUserId();
        return Result.success(teamService.findResponse(
                teamService.create(userId, request).getId(), userId));
    }

    /** 我的团队列表 */
    @GetMapping
    public Result<List<TeamResponse>> myTeams() {
        return Result.success(teamService.listMyTeams(AuthorizationPolicy.getCurrentUserId()));
    }

    /** 团队详情 */
    @GetMapping("/{id}")
    public Result<TeamResponse> detail(@PathVariable Long id) {
        return Result.success(teamService.findResponse(id, AuthorizationPolicy.getCurrentUserId()));
    }

    /** 更新团队信息（OWNER/ADMIN） */
    @PutMapping("/{id}")
    public Result<TeamResponse> update(@PathVariable Long id, @Valid @RequestBody TeamUpdateRequest request) {
        return Result.success(teamService.update(id, AuthorizationPolicy.getCurrentUserId(), request));
    }

    /** 解散团队（仅 OWNER） */
    @DeleteMapping("/{id}")
    public Result<Void> dissolve(@PathVariable Long id) {
        teamService.dissolve(id, AuthorizationPolicy.getCurrentUserId());
        return Result.success();
    }

    /* ==================== 成员管理 ==================== */

    /** 邀请成员（OWNER/ADMIN，可从好友列表选） */
    @PostMapping("/{id}/members")
    public Result<Void> invite(@PathVariable Long id, @Valid @RequestBody TeamInviteRequest request) {
        teamService.invite(id, AuthorizationPolicy.getCurrentUserId(), request.getUserIds());
        return Result.success();
    }

    /** 成员列表 */
    @GetMapping("/{id}/members")
    public Result<List<TeamMemberResponse>> members(@PathVariable Long id) {
        return Result.success(teamService.listMembers(id, AuthorizationPolicy.getCurrentUserId()));
    }

    /** 移除成员（OWNER/ADMIN，不能移除 OWNER 与自身） */
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        teamService.removeMember(id, AuthorizationPolicy.getCurrentUserId(), userId);
        return Result.success();
    }

    /** 退出团队（OWNER 不能退出） */
    @PostMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable Long id) {
        teamService.leave(id, AuthorizationPolicy.getCurrentUserId());
        return Result.success();
    }
}
