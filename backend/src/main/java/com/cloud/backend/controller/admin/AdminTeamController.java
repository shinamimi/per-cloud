package com.cloud.backend.controller.admin;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminTeamResponse;
import com.cloud.backend.dto.admin.QuotaRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.RecycleBinResponse;
import com.cloud.backend.dto.team.TeamMemberResponse;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.team.TeamFileService;
import com.cloud.backend.service.team.TeamService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamService teamService;
    private final TeamFileService teamFileService;

    public AdminTeamController(TeamService teamService, TeamFileService teamFileService) {
        this.teamService = teamService;
        this.teamFileService = teamFileService;
    }

    /**
     * 团队列表（含成员数统计）。
     */
    @GetMapping
    public Result<List<AdminTeamResponse>> listTeams() {
        List<AdminTeamResponse> teams = teamService.findAll().stream()
                .map(t -> new AdminTeamResponse(
                        t.getId(), t.getName(), t.getOwnerId(), t.getDescription(),
                        t.getStatus(), t.getQuota(), t.getUsedSpace(),
                        teamService.countMembers(t.getId()), t.getCreatedAt()))
                .toList();
        return Result.success(teams);
    }

    /**
     * 团队详情：基本信息 + 成员列表
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> teamDetail(@PathVariable Long id) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("team", teamService.findById(id));
        detail.put("members", teamService.adminListMembers(id));
        return Result.success(detail);
    }

    /**
     * 调整团队配额（设置管理端赠送额度，最终配额由服务层汇总计算）。
     */
    @PutMapping("/{id}/quota")
    public Result<Void> updateQuota(@PathVariable Long id, @RequestBody QuotaRequest request) {
        teamService.adminUpdateQuota(id, request.getAdminBonusQuota());
        return Result.success();
    }

    /**
     * 团队文件列表（管理端只读）
     */
    @GetMapping("/{id}/files")
    public Result<Page<FileNodeResponse>> teamFiles(@PathVariable Long id,
                                                    @RequestParam(required = false) Long parentId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(teamFileService.adminListFiles(id, parentId == null ? 0L : parentId, page, size));
    }

    /**
     * 团队回收站（管理端只读）
     */
    @GetMapping("/{id}/recycle-bin")
    public Result<List<RecycleBinResponse>> teamRecycleBin(@PathVariable Long id) {
        return Result.success(teamFileService.adminRecycleBin(id));
    }

    /**
     * 管理端物理清除团队回收站记录
     */
    @DeleteMapping("/{id}/recycle-bin/{recycleId}")
    public Result<Void> purgeRecycle(@PathVariable Long id, @PathVariable Long recycleId) {
        teamFileService.adminPurge(id, recycleId);
        return Result.success();
    }

    /**
     * 解散团队（管理端强制解散，记录当前操作者 ID 供审计）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> dissolveTeam(@PathVariable Long id,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        teamService.adminDissolve(id, loginUser.getUserId());
        return Result.success();
    }
}
