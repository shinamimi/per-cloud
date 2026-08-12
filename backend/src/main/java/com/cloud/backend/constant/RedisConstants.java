package com.cloud.backend.constant;

/**
 * Redis Key 常量 —— 文件模块上传/合并状态 + 分享模块。
 * 前缀说明：
 * - upload:meta:{uploadId}          Hash：上传元数据（文件名/大小/哈希/父目录/分片数）
 * - upload:chunks:{uploadId}        Set：已上传分片序号（断点续传）
 * - upload:uploading:{userId}       Set：用户进行中的上传任务（并发任务数限制）
 * - lock:merge:{uploadId}           合并分布式锁
 * - share:pwd-fail:{token}          String：提取码错误计数（满 5 次锁定）
 * - share:pwd-ok:{token}            String：提取码验证通过标记（访问文件树/下载/转存前校验）
 *
 * 修改指引：
 * - 【统一】修改上传/合并状态前缀    → UPLOAD_META_PREFIX / UPLOAD_CHUNKS_PREFIX / UPLOADING_PREFIX / MERGE_LOCK_PREFIX；
 *                             改前缀会使存量进行中的上传任务 Key 失效（断点续传与合并锁丢失），需评估存量数据；
 *                             改后需同步 Service 读写处（上传/合并状态读写、断点续传与合并锁逻辑）与存量 Key
 * - 【统一】修改分享提取码校验前缀   → SHARE_PWD_FAIL_PREFIX / SHARE_PWD_OK_PREFIX；改前缀后存量校验标记失效，用户需重新验证提取码；
 *                             改后需同步 Service 读写处（ShareServiceImpl 提取码校验）与存量 Key
 * - 【统一】修改下载去重前缀        → SHARE_DOWNLOAD_DEDUP_PREFIX；去重逻辑在 DownloadService 拼接使用，改前缀会重置当前去重窗口；
 *                             改后需同步 DownloadService 拼接处
 * - 【习惯】调整锁定次数/ TTL      → 本类只定义前缀；"满 5 次锁定"写死在 ShareServiceImpl，TTL（24h/60s）写在常量注释，
 *                             调整时需同步修改业务代码与该注释
 */
public interface RedisConstants {

    /** 【统一】改后需同步 Service 读写处（上传/合并状态读写、断点续传与合并锁逻辑）与存量 Key */
    String UPLOAD_META_PREFIX = "upload:meta:";
    String UPLOAD_CHUNKS_PREFIX = "upload:chunks:";
    String UPLOADING_PREFIX = "upload:uploading:";
    String MERGE_LOCK_PREFIX = "lock:merge:";

    /** 提取码错误计数（满 5 次锁定）；【统一】改后需同步 Service 读写处（ShareServiceImpl 提取码校验）与存量 Key */
    String SHARE_PWD_FAIL_PREFIX = "share:pwd-fail:";
    /** 提取码验证通过标记（TTL 24h）；【统一】改后需同步 Service 读写处（ShareServiceImpl 提取码校验）与存量 Key */
    String SHARE_PWD_OK_PREFIX = "share:pwd-ok:";

    /** 分享下载去重标记（share:dl-dedup:{shareId}:{snapshotId}:{clientIp}，TTL 60s，防止短时间重复下载刷计数）；【统一】改后需同步 DownloadService 拼接处 */
    String SHARE_DOWNLOAD_DEDUP_PREFIX = "share:dl-dedup:";
}
