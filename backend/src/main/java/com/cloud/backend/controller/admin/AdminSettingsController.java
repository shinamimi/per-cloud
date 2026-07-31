package com.cloud.backend.controller.admin;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.admin.AdminUploadLimitsRequest;
import com.cloud.backend.service.admin.AdminSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置控制器（管理端）。
 * 上传限制配置项（单文件大小上限、并发任务数，VIP 差异化）持久化到 t_setting，
 * 无配置时使用 application-local.yml 默认值。
 */
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    public AdminSettingsController(AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("defaultQuota", FileConstants.DEFAULT_QUOTA);
        settings.put("defaultChunkSize", FileConstants.DEFAULT_CHUNK_SIZE);
        settings.put("maxSizeUser", adminSettingsService.getMaxSizeUser());
        settings.put("maxSizeVip", adminSettingsService.getMaxSizeVip());
        settings.put("maxConcurrentUser", adminSettingsService.getMaxConcurrentUser());
        settings.put("maxConcurrentVip", adminSettingsService.getMaxConcurrentVip());
        return Result.success(settings);
    }

    @PutMapping("/upload")
    public Result<Void> updateUploadLimits(@RequestBody AdminUploadLimitsRequest request) {
        adminSettingsService.updateUploadLimits(
                request.getMaxSizeUser(),
                request.getMaxSizeVip(),
                request.getMaxConcurrentUser(),
                request.getMaxConcurrentVip());
        return Result.success();
    }
}
