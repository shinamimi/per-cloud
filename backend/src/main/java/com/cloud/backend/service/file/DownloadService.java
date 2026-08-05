package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.entity.File;

import java.util.List;

/**
 * 下载服务 —— 单文件 presigned URL；批量打包异步下载（WebSocket 通知）。
 *
 * 修改指引：
 * - 【习惯】想改"单文件 presigned 下载 URL 有效期" → getDownloadUrl()/getDownloadUrlForShare() 中取
 *   adminSettingsService.getDownloadLinkTtlMinutes()（默认 10 分钟）；改动影响前端直连对象存储的可用时长
 * - 【习惯】想改"个人文件下载校验规则" → getDownloadUrl() 对应 DownloadServiceImpl 的 getOwnedFile() + requireEnabled()
 *   （目录/空文件拒绝、DISABLED 或对象级禁用拒绝）；改动影响用户端可下载范围，管理端 detailEntity 路径不受限
 * - 【习惯】想改"批量打包流程（目录递归展开 + 本地临时 zip → 上传对象存储 → 预签名 URL）" →
 *   createBatchTask()/createBatchTaskForGuest() 对应 pack()/writeZipEntry()；改动影响打包产物、下载地址与
 *   WebSocket 进度推送
 * - 【习惯】想改"打包产物过期清理" → cleanupExpiredPackages()（fileProperties.getPackageExpireHours()，由定时任务调用，
 *   删 MinIO zip + 清内存任务）；改动影响存储与内存占用
 * - 【习惯】并发/内存态：打包任务存进程内存（ConcurrentHashMap），pack() 在独立线程执行并广播进度；
 *   改共享状态访问须保持线程安全，服务重启任务不恢复
 * - 【习惯】新增方法 → 需同步实现类 DownloadServiceImpl 及 FileController/ShareController 调用方
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

    /** 分享下载：已由调用方完成分享状态/归属校验，直接生成 presigned URL */
    String getDownloadUrlForShare(File file);

    /** 分享批量打包：files 已由调用方收集（快照展开），直接复用打包流水线 */
    BatchDownloadResponse createBatchTaskForGuest(List<File> files);
}
