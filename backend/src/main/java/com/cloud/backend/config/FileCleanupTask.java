package com.cloud.backend.config;

import com.cloud.backend.constant.RedisConstants;
import com.cloud.backend.service.file.DownloadService;
import com.cloud.backend.service.file.RecycleBinService;
import com.cloud.backend.service.file.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件清理定时任务（file-module.md 第四节/五节/七节）。
 * - 每日 03:00：回收站过期记录物理清理（30 天）+ 打包产物清理（24 小时）
 * - 每日 04:00：上传临时分片清理（Redis 元数据已过期的孤儿对象）
 */
@Component
public class FileCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupTask.class);

    private final RecycleBinService recycleBinService;
    private final DownloadService downloadService;
    private final StorageService storageService;
    private final StringRedisTemplate redis;

    public FileCleanupTask(RecycleBinService recycleBinService, DownloadService downloadService,
                           StorageService storageService, StringRedisTemplate redis) {
        this.recycleBinService = recycleBinService;
        this.downloadService = downloadService;
        this.storageService = storageService;
        this.redis = redis;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredData() {
        log.info("Scheduled cleanup: recycle bin expired records");
        recycleBinService.purgeExpired();
        downloadService.cleanupExpiredPackages();
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
