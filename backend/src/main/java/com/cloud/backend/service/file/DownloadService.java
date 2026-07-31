package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.BatchDownloadResponse;

import java.util.List;

/**
 * 下载服务 —— 单文件 presigned URL；批量打包异步下载（WebSocket 通知）。
 */
public interface DownloadService {

    /** 生成 presigned 下载 URL（10 分钟有效） */
    String getDownloadUrl(Long userId, Long fileId);

    /** 创建批量打包任务（异步执行） */
    BatchDownloadResponse createBatchTask(Long userId, List<Long> fileIds);

    /** 查询打包任务状态 */
    BatchDownloadResponse getBatchTask(String taskId);

    /** 清理过期打包产物（定时任务） */
    void cleanupExpiredPackages();
}
