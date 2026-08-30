package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.entity.File;

import java.util.List;

public interface DownloadService {

    /** 生成 presigned 下载 URL（10 分钟有效） */
    String getDownloadUrl(Long userId, Long fileId);

    /** 创建批量打包任务（异步执行） */
    BatchDownloadResponse createBatchTask(Long userId, List<Long> fileIds);

    /** 查询打包任务状态 */
    BatchDownloadResponse getBatchTask(String taskId);

    /** 清理过期打包产物（定时任务） */
    void cleanupExpiredPackages();

    /** 分享下载：已由调用方完成分享状态/归属校验，直接生成 presigned URL */
    String getDownloadUrlForShare(File file);

    /** 分享批量打包：files 已由调用方收集（快照展开），直接复用打包流水线 */
    BatchDownloadResponse createBatchTaskForGuest(List<File> files);
}
