package com.cloud.backend.controller;

import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.share.*;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.service.share.ShareService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 访客分享访问 —— /api/shares/access/**（公开，无需登录；SecurityConfig permitAll）。
 * 提取码验证 → 文件树/预览/下载/批量下载/转存。
 * 转存需登录（save 接口内校验）。
 */
@RestController
@RequestMapping("/api/shares/access")
public class GuestShareController {

    private final ShareService shareService;

    public GuestShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /** 分享信息（requirePassword=true 时前端弹提取码框） */
    @GetMapping("/{token}")
    public Result<GuestShareInfoResponse> info(@PathVariable String token) {
        return Result.success(shareService.getAccessInfo(token));
    }

    /** 验证提取码（错误限次 5 次，Redis 计数） */
    @PostMapping("/{token}/verify")
    public Result<Void> verify(@PathVariable String token, @RequestBody ShareVerifyRequest request) {
        shareService.verifyPassword(token, request == null ? null : request.getPassword());
        return Result.success();
    }

    /** 分享文件树（平铺快照节点） */
    @GetMapping("/{token}/files")
    public Result<List<ShareFileNodeResponse>> files(@PathVariable String token) {
        return Result.success(shareService.getShareFiles(token));
    }

    /** 预览（不计数） */
    @GetMapping("/{token}/file/{snapshotId}/preview")
    public Result<FilePreviewResponse> preview(@PathVariable String token, @PathVariable Long snapshotId) {
        return Result.success(shareService.previewShareFile(token, snapshotId));
    }

    /** 下载（download_count +1，达限置 EXHAUSTED） */
    @GetMapping("/{token}/file/{snapshotId}/download")
    public void download(@PathVariable String token, @PathVariable Long snapshotId, HttpServletResponse response) {
        String url = shareService.getShareDownloadUrl(token, snapshotId);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    /** 批量打包下载（一次下载动作计数 +1） */
    @PostMapping("/{token}/batch-download")
    public Result<BatchDownloadResponse> batchDownload(@PathVariable String token,
                                                       @RequestBody(required = false) ShareAccessRequest request) {
        List<Long> ids = request == null ? List.of() : request.getSnapshotIds();
        return Result.success(shareService.batchDownload(token, ids));
    }

    /** 批量打包任务查询（taskId 为随机 UUID，公开可接受） */
    @GetMapping("/{token}/batch-task/{taskId}")
    public Result<BatchDownloadResponse> batchTask(@PathVariable String token, @PathVariable String taskId) {
        return Result.success(shareService.getBatchTask(taskId));
    }

    /** 转存到个人空间（需登录；复用秒传引用计数 +1） */
    @PostMapping("/{token}/save")
    public Result<Void> save(@PathVariable String token, @RequestBody(required = false) ShareAccessRequest request) {
        Long userId = AuthorizationPolicy.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<Long> ids = request == null ? List.of() : request.getSnapshotIds();
        shareService.saveShareFiles(userId, token, ids);
        return Result.success();
    }
}
