package com.cloud.backend.controller;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileCopyRequest;
import com.cloud.backend.dto.file.FileMoveRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.file.FileRenameRequest;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.dto.file.RecycleBinResponse;
import com.cloud.backend.service.team.TeamFileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 团队文件控制器 —— 团队文件管理 + 团队回收站。
 * 上传/秒传复用 /api/files/upload/*（请求体带 teamId），不重复实现。
 * 下载/预览走团队鉴权（成员均可）；改/删按权限矩阵（MEMBER 只能操作自己上传的）。
 *
 * 修改指引：
 * - 【习惯】列表 / 树 / 目录     → GET /api/teams/{teamId}/files、/tree、POST /directory；
 *                         调 teamFileService.listFiles / tree / createDirectory；成员均可访问（服务内校验成员资格）
 * - 【习惯】改名 / 移动 / 复制 / 删除 → PUT /{fileId}/rename、POST /{fileId}/move、POST /{fileId}/copy、DELETE /{fileId}；
 *                         调 rename / move / copy / deleteToRecycle；MEMBER 只能操作自己上传的文件
 * - 【习惯】下载 / 预览         → GET /{fileId}/download（302 预签名重定向）、GET /{fileId}/preview；成员均可
 * - 【习惯】团队回收站          → GET /recycle-bin、POST /recycle-bin/{recycleId}/restore、DELETE /recycle-bin/{recycleId}；
 *                         调 teamFileService.recycleBin / restore / purge
 * - 【习惯】上传 / 秒传         → 复用 FileController 的 /api/files/upload/*（请求体带 teamId），不在此类实现，
 *                         改动上传逻辑请改 FileController 与 UploadService
 * - 【习惯】新增/修改接口       → 当前用户通过 @AuthenticationPrincipal LoginUser 获取（区别于用 AuthorizationPolicy 的控制器）；
 *                         需登录，若为公开接口须在 SecurityConfig 放行并同步前端 API 层
 * - 【习惯】分页参数            → list 的 page（默认 1）、size（默认 20），改动需同步前端分页组件
 */
@RestController
@RequestMapping("/api/teams/{teamId}/files")
public class TeamFileController {

    private final TeamFileService teamFileService;

    public TeamFileController(TeamFileService teamFileService) {
        this.teamFileService = teamFileService;
    }

    /* ==================== 列表 / 树 / 目录 ==================== */

    @GetMapping
    public Result<Page<FileNodeResponse>> list(@PathVariable Long teamId,
                                               @RequestParam(required = false) Long parentId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                       com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.listFiles(teamId, loginUser.getUserId(),
                parentId == null ? 0L : parentId, page, size));
    }

    @GetMapping("/tree")
    public Result<List<FileTreeResponse>> tree(@PathVariable Long teamId,
                                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                       com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.tree(teamId, loginUser.getUserId()));
    }

    @PostMapping("/directory")
    public Result<FileNodeResponse> createDirectory(@PathVariable Long teamId,
                                                    @Valid @RequestBody DirectoryCreateRequest request,
                                                    @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                            com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.createDirectory(teamId, loginUser.getUserId(), request));
    }

    /* ==================== 重命名 / 移动 / 复制 / 删除 ==================== */

    @PutMapping("/{fileId}/rename")
    public Result<FileNodeResponse> rename(@PathVariable Long teamId, @PathVariable Long fileId,
                                           @Valid @RequestBody FileRenameRequest request,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                   com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.rename(teamId, loginUser.getUserId(), fileId, request.getName()));
    }

    @PostMapping("/{fileId}/move")
    public Result<FileNodeResponse> move(@PathVariable Long teamId, @PathVariable Long fileId,
                                         @Valid @RequestBody FileMoveRequest request,
                                         @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                 com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.move(teamId, loginUser.getUserId(), fileId, request.getTargetParentId()));
    }

    @PostMapping("/{fileId}/copy")
    public Result<FileNodeResponse> copy(@PathVariable Long teamId, @PathVariable Long fileId,
                                         @Valid @RequestBody FileCopyRequest request,
                                         @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                 com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.copy(teamId, loginUser.getUserId(), fileId, request.getTargetParentId()));
    }

    @DeleteMapping("/{fileId}")
    public Result<Void> delete(@PathVariable Long teamId, @PathVariable Long fileId,
                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                                       com.cloud.backend.security.LoginUser loginUser) {
        teamFileService.deleteToRecycle(teamId, loginUser.getUserId(), fileId);
        return Result.success();
    }

    /* ==================== 下载 / 预览 ==================== */

    @GetMapping("/{fileId}/download")
    public void download(@PathVariable Long teamId, @PathVariable Long fileId,
                         HttpServletResponse response,
                         @org.springframework.security.core.annotation.AuthenticationPrincipal
                                 com.cloud.backend.security.LoginUser loginUser) {
        String url = teamFileService.getDownloadUrl(teamId, loginUser.getUserId(), fileId);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    @GetMapping("/{fileId}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable Long teamId, @PathVariable Long fileId,
                                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                       com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.preview(teamId, loginUser.getUserId(), fileId));
    }

    /* ==================== 团队回收站 ==================== */

    @GetMapping("/recycle-bin")
    public Result<List<RecycleBinResponse>> recycleBin(@PathVariable Long teamId,
                                                       @org.springframework.security.core.annotation.AuthenticationPrincipal
                                                               com.cloud.backend.security.LoginUser loginUser) {
        return Result.success(teamFileService.recycleBin(teamId, loginUser.getUserId()));
    }

    @PostMapping("/recycle-bin/{recycleId}/restore")
    public Result<Void> restore(@PathVariable Long teamId, @PathVariable Long recycleId,
                                @org.springframework.security.core.annotation.AuthenticationPrincipal
                                        com.cloud.backend.security.LoginUser loginUser) {
        teamFileService.restore(teamId, loginUser.getUserId(), recycleId);
        return Result.success();
    }

    @DeleteMapping("/recycle-bin/{recycleId}")
    public Result<Void> purge(@PathVariable Long teamId, @PathVariable Long recycleId,
                              @org.springframework.security.core.annotation.AuthenticationPrincipal
                                      com.cloud.backend.security.LoginUser loginUser) {
        teamFileService.purge(teamId, loginUser.getUserId(), recycleId);
        return Result.success();
    }
}
