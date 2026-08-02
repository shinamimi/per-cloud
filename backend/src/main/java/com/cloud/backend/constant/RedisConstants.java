package com.cloud.backend.constant;

/**
 * Redis Key 常量 —— 文件模块上传/合并状态 + 分享模块。
 * 前缀说明：
 * - upload:meta:{uploadId}          Hash：上传元数据（文件名/大小/哈希/父目录/分片数）
 * - upload:chunks:{uploadId}        Set：已上传分片序号（断点续传）
 * - upload:uploading:{userId}       Set：用户进行中的上传任务（并发任务数限制）
 * - lock:merge:{uploadId}           合并分布式锁
 * - share:pwd-fail:{token}          String：提取码错误计数（>=5 锁定，docs/share-module.md §5）
 * - share:pwd-ok:{token}            String：提取码验证通过标记（访问文件树/下载/转存前校验）
 */
public interface RedisConstants {

    String UPLOAD_META_PREFIX = "upload:meta:";
    String UPLOAD_CHUNKS_PREFIX = "upload:chunks:";
    String UPLOADING_PREFIX = "upload:uploading:";
    String MERGE_LOCK_PREFIX = "lock:merge:";

    /** 提取码错误计数（满 5 次锁定） */
    String SHARE_PWD_FAIL_PREFIX = "share:pwd-fail:";
    /** 提取码验证通过标记（TTL 24h） */
    String SHARE_PWD_OK_PREFIX = "share:pwd-ok:";

    /** 分享下载去重标记（share:dl-dedup:{shareId}:{snapshotId}:{clientIp}，TTL 60s，防止短时间重复下载刷计数） */
    String SHARE_DOWNLOAD_DEDUP_PREFIX = "share:dl-dedup:";
}
