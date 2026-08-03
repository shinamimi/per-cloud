package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.DisabledObjectMapper;
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
 * 下载服务实现 —— 单文件直链下载与批量打包下载。
 *
 * 设计思路：
 * - 单文件：生成带有效期（管理员配置分钟数）的预签名 URL，前端直连对象存储，不占后端带宽
 * - 批量：异步打包任务（本地临时 zip → 上传对象存储），进度经 WebSocket 推送，完成时返回预签名 URL
 * - 打包产物小时级过期（配置项），由定时任务清理内存任务与存储对象
 * - 禁用/对象级禁用文件对用户端不可下载，管理员后台不受此限
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
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;
    private final DisabledObjectMapper disabledObjectMapper;

    private final Map<String, BatchTask> tasks = new ConcurrentHashMap<>();

    public DownloadServiceImpl(FileService fileService, FileMapper fileMapper, StorageService storageService,
                               FileProperties fileProperties, ProgressWebSocketHandler progressHandler,
                               com.cloud.backend.service.admin.AdminSettingsService adminSettingsService,
                               DisabledObjectMapper disabledObjectMapper) {
        this.fileService = fileService;
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.fileProperties = fileProperties;
        this.progressHandler = progressHandler;
        this.adminSettingsService = adminSettingsService;
        this.disabledObjectMapper = disabledObjectMapper;
        this.packExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "pack-task");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 生成用户个人文件的预签名下载 URL。
     * 前置条件：文件归属该用户且未删除；目录/空文件与禁用文件拒绝。
     */
    @Override
    public String getDownloadUrl(Long userId, Long fileId) {
        File file = fileService.getOwnedFile(userId, fileId);
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "目录或空文件不可下载");
        }
        requireEnabled(userId, file);
        try {
            return storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    /**
     * 创建批量打包任务（异步）：目录递归展开为其下全部正常文件，文件级校验归属与禁用状态；
     * 任务立即返回，打包进度与结果经 WebSocket 推送。
     */
    @Override
    public BatchDownloadResponse createBatchTask(Long userId, List<Long> fileIds) {
        List<File> files = new ArrayList<>();
        for (Long fileId : fileIds) {
            File file = fileService.getOwnedFile(userId, fileId);
            if (file.isDir()) {
                collectFiles(userId, file.getId(), files);
            } else {
                requireEnabled(userId, file);
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

    /** 生成分享文件的预签名下载 URL（分享访问的鉴权与下载计数在调用方完成）。 */
    @Override
    public String getDownloadUrlForShare(File file) {
        try {
            return storageService.generateDownloadUrl(file.getObjectName(),
                    adminSettingsService.getDownloadLinkTtlMinutes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    /** 创建访客（分享）批量打包任务：文件清单由调用方传入并完成校验，打包过程与普通批量任务一致。 */
    @Override
    public BatchDownloadResponse createBatchTaskForGuest(List<File> files) {
        if (files.isEmpty()) {
            throw new BusinessException(ErrorCode.BATCH_TASK_NOT_FOUND, "没有可打包的文件");
        }
        BatchTask task = new BatchTask();
        task.status = "PENDING";
        task.files = files;
        task.total = files.size();
        task.done = 0;
        task.createdAt = System.currentTimeMillis();
        tasks.put(task.taskId, task);
        packExecutor.execute(() -> pack(task));
        return toResponse(task);
    }

    /** 查询打包任务状态（内存任务表）；任务不存在抛业务异常。 */
    @Override
    public BatchDownloadResponse getBatchTask(String taskId) {
        BatchTask task = tasks.get(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.BATCH_TASK_NOT_FOUND);
        }
        return toResponse(task);
    }

    /** 清理过期打包产物：删除对象存储中的 zip 并移除内存任务（供定时任务调用）。 */
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

    /** 后台执行打包：逐文件写入本地临时 zip → 上传对象存储 → 广播完成/失败状态；失败保留任务状态供前端查询。 */
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
            task.url = storageService.generateDownloadUrl(task.objectName, adminSettingsService.getDownloadLinkTtlMinutes());
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

    /** 将单个文件写入 zip：从对象存储流式读取，条目名含目录层级且重名自动加序号。 */
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

    /** BFS 收集目录下所有文件（不含子目录本身；禁用文件不打包） */
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
                } else if (child.getStatus() == com.cloud.backend.enums.FileStatus.NORMAL) {
                    result.add(child);
                }
            }
        }
    }

    /** 禁用/对象级禁用文件对用户端不可下载（管理员后台不受此限） */
    private void requireEnabled(Long userId, File file) {
        if (file.getStatus() == com.cloud.backend.enums.FileStatus.DISABLED) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        if (file.getFileHash() != null && !file.getFileHash().isEmpty()
                && disabledObjectMapper.countBlocked(file.getFileHash(), userId) > 0) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
    }

    /** 组装任务响应 DTO（状态、总数、已完成数、结果 URL）。 */
    private BatchDownloadResponse toResponse(BatchTask task) {
        return BatchDownloadResponse.of(task.taskId, task.status, task.total, task.done, task.url);
    }

    /**
     * 内存中的批量打包任务状态（进程内存态：任务注册、进度与结果查询均由此驱动，
     * 服务重启后任务不恢复）。
     */
    private static class BatchTask {
        final String taskId = IdUtil.simpleUUID();
        Long userId;
        /** PENDING / PACKING / DONE / FAILED */
        String status;
        List<File> files;
        int total;
        int done;
        String url;
        String objectName;
        /** 单位：毫秒 */
        long createdAt;
    }
}
