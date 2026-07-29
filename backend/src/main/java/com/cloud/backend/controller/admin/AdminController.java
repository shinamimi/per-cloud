package com.cloud.backend.controller.admin;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.File;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.Share;
import com.cloud.backend.entity.User;
import com.cloud.backend.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台主控制器 —— 仪表盘、文件管理、分享管理、日志、设置。
 *
 * 路由权限：/api/admin/** 需要 ADMIN 或以上角色
 * 实现原理：SecurityConfig 中配置 .requestMatchers("/api/admin/**").hasRole("ADMIN")
 * Spring Security 检查 LoginUser.getAuthorities() 是否包含 "ROLE_ADMIN" 或更高权限。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final FileService fileService;
    private final ShareService shareService;
    private final OperationLogService operationLogService;
    private final StorageService storageService;

    public AdminController(UserService userService,
                           FileService fileService,
                           ShareService shareService,
                           OperationLogService operationLogService,
                           StorageService storageService) {
        this.userService = userService;
        this.fileService = fileService;
        this.shareService = shareService;
        this.operationLogService = operationLogService;
        this.storageService = storageService;
    }

    /** 仪表盘统计 —— 用户数、文件数、总容量、使用率 */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> stats() {
        List<User> users = userService.findAll();
        List<File> files = fileService.findAll();

        long userCount = users.size();
        long fileCount = files.size();
        long totalSize = files.stream().mapToLong(File::getSize).sum();
        long totalQuota = users.stream().mapToLong(User::getQuota).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("fileCount", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("totalQuota", totalQuota);
        stats.put("usagePercent", totalQuota > 0 ? (double) totalSize / totalQuota * 100 : 0);
        return Result.success(stats);
    }

    /** 文件列表 —— 查看所有用户的所有文件 */
    @GetMapping("/files")
    public Result<List<File>> listFiles() {
        return Result.success(fileService.findAll());
    }

    /** 删除文件 —— 同步清理 MinIO 对象 */
    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        File file = fileService.findById(id);
        if (file == null) {
            return Result.fail(ErrorCode.FILE_NOT_FOUND);
        }
        String objectName = file.getObjectName();
        if (objectName != null && !objectName.isEmpty()) {
            storageService.delete(objectName);
        }
        fileService.removeById(id);
        return Result.success();
    }

    /** 分享列表 */
    @GetMapping("/shares")
    public Result<List<Share>> listShares() {
        return Result.success(shareService.findAll());
    }

    /** 取消分享 */
    @PostMapping("/shares/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        Share share = shareService.findById(id);
        if (share == null) {
            return Result.fail(ErrorCode.SHARE_NOT_FOUND);
        }
        share.setStatus(com.cloud.backend.enums.ShareStatus.CANCELED);
        shareService.update(share);
        return Result.success();
    }

    /** 操作日志列表 */
    @GetMapping("/logs")
    public Result<List<OperationLog>> listLogs() {
        return Result.success(operationLogService.listAll());
    }

    /** 系统设置 —— 返回默认配额和分片大小等全局常量 */
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultQuota", FileConstants.DEFAULT_QUOTA);
        settings.put("defaultChunkSize", FileConstants.DEFAULT_CHUNK_SIZE);
        return Result.success(settings);
    }
}