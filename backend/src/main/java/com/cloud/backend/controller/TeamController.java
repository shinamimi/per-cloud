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
