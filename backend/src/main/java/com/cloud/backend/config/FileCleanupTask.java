package com.cloud.backend.config;

import com.cloud.backend.constant.RedisConstants;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.mapper.OperationLogMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.file.DownloadService;
import com.cloud.backend.service.file.RecycleBinService;
import com.cloud.backend.service.file.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupTask.class);

    private final RecycleBinService recycleBinService;
    private final DownloadService downloadService;
    private final StorageService storageService;
    private final StringRedisTemplate redis;
    private final OperationLogMapper operationLogMapper;
    private final AdminSettingsService settingsService;

    public FileCleanupTask(RecycleBinService recycleBinService, DownloadService downloadService,
                           StorageService storageService, StringRedisTemplate redis,
                           OperationLogMapper operationLogMapper, AdminSettingsService settingsService) {
        this.recycleBinService = recycleBinService;
        this.downloadService = downloadService;
        this.storageService = storageService;
        this.redis = redis;
        this.operationLogMapper = operationLogMapper;
        this.settingsService = settingsService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredData() {
        log.info("Scheduled cleanup: recycle bin expired records");
        recycleBinService.purgeExpired();
        downloadService.cleanupExpiredPackages();
    }

    /** 日志保留天数清理：登录日志用 log.login-days，其余操作日志用 log.operation-days */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanupExpiredLogs() {
        log.info("Scheduled cleanup: expired operation logs");
        LocalDateTime operationCutoff = LocalDateTime.now().minusDays(settingsService.getOperationLogDays());
        LocalDateTime loginCutoff = LocalDateTime.now().minusDays(settingsService.getLoginLogDays());
        int loginDeleted = operationLogMapper.deleteByCreatedAtBefore(loginCutoff, OperationType.LOGIN.name());
        int operationDeleted = operationLogMapper.deleteByCreatedAtBefore(operationCutoff, "NON_LOGIN");
        if (loginDeleted + operationDeleted > 0) {
            log.info("Expired logs deleted: {} login, {} operation", loginDeleted, operationDeleted);
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupStaleUploads() {
        log.info("Scheduled cleanup: stale upload chunks");
        try {
            List<String> objects = storageService.listObjects("uploads/");
            int deleted = 0;
            for (String object : objects) {
                // uploads/{userId}/{uploadId}/chunk_{seq}
                String[] parts = object.split("/");
                if (parts.length < 4) {
                    continue;
                }
                String uploadId = parts[2];
                Boolean alive = redis.hasKey(RedisConstants.UPLOAD_META_PREFIX + uploadId);
                if (!Boolean.TRUE.equals(alive)) {
                    storageService.delete(object);
                    deleted++;
                }
            }
            if (deleted > 0) {
                log.info("Stale upload chunks deleted: {}", deleted);
            }
        } catch (RuntimeException e) {
            log.warn("Cleanup stale uploads failed", e);
        }
    }
}

