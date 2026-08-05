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

/**
 * 后台团队管理控制器 —— 团队列表/详情、配额调整、文件与回收站只读查看、解散团队。
 *
 * 设计思路：
 * 1. 列表聚合成员数指标（countMembers），避免前端逐队查询
 * 2. 文件/回收站接口为管理端只读视角，仅供查看与治理，不参与成员权限判定
 * 3. 解散团队为强制操作，记录执行操作者信息以便审计
 *
 * 修改指引：
 * - 【习惯】团队列表 / 详情      → GET /api/admin/teams、/{id}，调 teamService.findAll / findById / adminListMembers；
 *                          权限 OPERATOR+（SecurityConfig /api/admin/**），列表聚合成员数指标
 * - 【习惯】调整配额           → PUT /api/admin/teams/{id}/quota，调 teamService.adminUpdateQuota(adminBonusQuota)；
 *                        最终配额由服务层汇总计算
 * - 【习惯】团队文件 / 回收站只读 → GET /api/admin/teams/{id}/files（分页）、/{id}/recycle-bin，
 *                          调 teamFileService.adminListFiles / adminRecycleBin；管理端只读视角，不参与成员权限判定
 * - 【习惯】清除回收站记录      → DELETE /api/admin/teams/{id}/recycle-bin/{recycleId}，调 adminPurge；物理清除
 * - 【习惯】解散团队           → DELETE /api/admin/teams/{id}，调 teamService.adminDissolve；强制解散，记录当前操作者 ID 供审计
 * - 【习惯】新增/修改接口       → 注意 SecurityConfig 权限级别并同步前端管理端 API 层；分页参数 page/size 改动需同步前端
 */
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
