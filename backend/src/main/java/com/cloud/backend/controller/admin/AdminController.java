package com.cloud.backend.controller.admin;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Result;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.Share;
import com.cloud.backend.entity.User;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.share.ShareService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final FileService fileService;
    private final ShareService shareService;
    private final OperationLogService operationLogService;

    public AdminController(UserService userService,
                           FileService fileService,
                           ShareService shareService,
                           OperationLogService operationLogService) {
        this.userService = userService;
        this.fileService = fileService;
        this.shareService = shareService;
        this.operationLogService = operationLogService;
    }

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

    @GetMapping("/files")
    public Result<List<File>> listFiles() {
        return Result.success(fileService.findAll());
    }

    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.adminDeleteFile(id);
        return Result.success();
    }

    @GetMapping("/shares")
    public Result<List<Share>> listShares() {
        return Result.success(shareService.findAll());
    }

    @PostMapping("/shares/{id}/cancel")
    public Result<Void> cancelShare(@PathVariable Long id) {
        shareService.adminCancelShare(id);
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> listLogs() {
        return Result.success(operationLogService.listAll());
    }

    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultQuota", FileConstants.DEFAULT_QUOTA);
        settings.put("defaultChunkSize", FileConstants.DEFAULT_CHUNK_SIZE);
        return Result.success(settings);
    }
}
