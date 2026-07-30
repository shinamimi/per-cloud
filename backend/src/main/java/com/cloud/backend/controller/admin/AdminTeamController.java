package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminTeamResponse;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.team.TeamService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamService teamService;

    public AdminTeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public Result<List<AdminTeamResponse>> listTeams() {
        List<AdminTeamResponse> teams = teamService.findAll().stream()
                .map(t -> new AdminTeamResponse(
                        t.getId(), t.getName(), t.getOwnerId(), t.getDescription(),
                        t.getStatus(), t.getQuota(), t.getUsedSpace(),
                        0, t.getCreatedAt()))
                .toList();
        return Result.success(teams);
    }

    @DeleteMapping("/{id}")
    public Result<Void> dissolveTeam(@PathVariable Long id,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        teamService.dissolve(id, loginUser.getUserId());
        return Result.success();
    }
}
