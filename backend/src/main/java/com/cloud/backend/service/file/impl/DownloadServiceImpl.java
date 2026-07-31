package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.service.file.DownloadService;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.utils.IdUtil;
import com.cloud.backend.websocket.ProgressWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 下载服务实现。
 *
 * 设计思路（file-module.md 第五节）：
 * - 单文件：生成 presigned URL（10 分钟）→ 302 重定向，前端直连 MinIO，不占后端带宽
 * - 批量：异步打包任务（本地临时 zip → 上传 packages/{taskId}.zip），
 *   进度通过 /ws/progress 推送，完成时返回 presigned URL
 * - 打包产物 24 小时过期，定时任务清理
 */
@Service
public class DownloadServiceImpl implements DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    private final FileService fileService;
    private final FileMapper fileMapper;
    private final StorageService storageService;
    private final FileProperties fileProperties;
    private final ProgressWebSocketHandler progressHandler;
    private final ExecutorService packExecutor;

    private final Map<String, BatchTask> tasks = new ConcurrentHashMap<>();

    public DownloadServiceImpl(FileService fileService, FileMapper fileMapper, StorageService storageService,
                               FileProperties fileProperties, ProgressWebSocketHandler progressHandler) {
        this.fileService = fileService;
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.fileProperties = fileProperties;
        this.progressHandler = progressHandler;
        this.packExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "pack-task");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public String getDownloadUrl(Long userId, Long fileId) {
        File file = fileService.getOwnedFile(userId, fileId);
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "目录或空文件不可下载");
        }
        try {
            return storageService.generateDownloadUrl(file.getObjectName(), 10);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    @Override
    public BatchDownloadResponse createBatchTask(Long userId, List<Long> fileIds) {
        List<File> files = new ArrayList<>();
        for (Long fileId : fileIds) {
            File file = fileService.getOwnedFile(userId, fileId);
            if (file.isDir()) {
                collectFiles(userId, file.getId(), files);
            } else {
                files.add(file);
            }
        }
        if (files.isEmpty()) {
            throw new BusinessException(ErrorCode.BATCH_TASK_NOT_FOUND, "没有可打包的文件");
        }
        BatchTask task = new BatchTask();
        task.userId = userId;
        task.status = "PENDING";
        task.files = files;
        task.total = files.size();
        task.done = 0;
        task.createdAt = System.currentTimeMillis();
        tasks.put(task.taskId, task);
        packExecutor.execute(() -> pack(task));
        return toResponse(task);
    }

    @Override
    public BatchDownloadResponse getBatchTask(String taskId) {
        BatchTask task = tasks.get(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.BATCH_TASK_NOT_FOUND);
        }
        return toResponse(task);
    }

    @Override
    public void cleanupExpiredPackages() {
        long expireMillis = fileProperties.getPackageExpireHours() * 3600_000L;
        long now = System.currentTimeMillis();
        for (BatchTask task : tasks.values()) {
            if (now - task.createdAt > expireMillis) {
                if (task.objectName != null && !task.objectName.isEmpty()) {
                    try {
                        storageService.delete(task.objectName);
                    } catch (RuntimeException e) {
                        log.warn("Delete expired package failed: {}", task.objectName, e);
                    }
                }
                tasks.remove(task.taskId);
            }
        }
    }

    /* ==================== 打包 ==================== */

    private void pack(BatchTask task) {
        task.status = "PACKING";
        progressHandler.broadcast("download", Map.of("taskId", task.taskId, "status", "PACKING",
                "total", task.total, "done", 0));
        java.io.File tempFile = null;
        try {
            tempFile = java.io.File.createTempFile("cloud-pack-", ".zip");
            Set<String> usedNames = new HashSet<>();
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tempFile))) {
                for (File file : task.files) {
                    writeZipEntry(zip, file, usedNames);
                    task.done++;
                    progressHandler.broadcast("download", Map.of("taskId", task.taskId, "status", "PACKING",
                            "total", task.total, "done", task.done));
                }
            }
            long size = tempFile.length();
            task.objectName = IdUtil.packageObject(task.taskId);
            try (FileInputStream input = new FileInputStream(tempFile)) {
                storageService.upload(task.objectName, input, size, "application/zip");
            }
            task.url = storageService.generateDownloadUrl(task.objectName, 10);
            task.status = "DONE";
            progressHandler.broadcast("download", Map.of("taskId", task.taskId, "status", "DONE",
                    "total", task.total, "done", task.done, "url", task.url));
        } catch (Exception e) {
            task.status = "FAILED";
            log.error("Batch pack failed: taskId={}", task.taskId, e);
            progressHandler.broadcast("download", Map.of("taskId", task.taskId, "status", "FAILED"));
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void writeZipEntry(ZipOutputStream zip, File file, Set<String> usedNames) throws IOException {
        String entryName = toEntryName(file.getPath(), usedNames);
        try (InputStream input = storageService.download(file.getObjectName())) {
            zip.putNextEntry(new ZipEntry(entryName));
            input.transferTo(zip);
            zip.closeEntry();
        }
    }

    /** zip 条目名：path 去掉首 "/"（保留目录层级）；重名自动加序号 */
    private String toEntryName(String path, Set<String> usedNames) {
        String base = path == null || path.isEmpty() ? "file" : path.startsWith("/") ? path.substring(1) : path;
        String name = base;
        int suffix = 2;
        while (!usedNames.add(name)) {
            int dot = base.lastIndexOf('.');
            String stem = dot > 0 ? base.substring(0, dot) : base;
            String ext = dot > 0 ? base.substring(dot) : "";
            name = stem + "（" + suffix + "）" + ext;
            suffix++;
        }
        return name;
    }

    /** BFS 收集目录下所有文件（不含子目录本身） */
    private void collectFiles(Long userId, Long dirId, List<File> result) {
        Map<Long, List<File>> childrenByParent = fileMapper.findByUserId(userId).stream()
                .collect(java.util.stream.Collectors.groupingBy(File::getParentId));
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(dirId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (File child : childrenByParent.getOrDefault(current, List.of())) {
                if (child.isDir()) {
                    queue.add(child.getId());
                } else {
                    result.add(child);
                }
            }
        }
    }

    private BatchDownloadResponse toResponse(BatchTask task) {
        return BatchDownloadResponse.of(task.taskId, task.status, task.total, task.done, task.url);
    }

    private static class BatchTask {
        final String taskId = IdUtil.simpleUUID();
        Long userId;
        String status;
        List<File> files;
        int total;
        int done;
        String url;
        String objectName;
        long createdAt;
    }
}
