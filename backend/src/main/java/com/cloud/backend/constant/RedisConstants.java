package com.cloud.backend.constant;

/**
 * Redis Key 常量 —— 文件模块上传/合并状态。
 * 前缀说明：
 * - upload:meta:{uploadId}          Hash：上传元数据（文件名/大小/哈希/父目录/分片数）
 * - upload:chunks:{uploadId}        Set：已上传分片序号（断点续传）
 * - upload:uploading:{userId}       Set：用户进行中的上传任务（并发任务数限制）
 * - lock:merge:{uploadId}           合并分布式锁
 */
public interface RedisConstants {

    String UPLOAD_META_PREFIX = "upload:meta:";
    String UPLOAD_CHUNKS_PREFIX = "upload:chunks:";
    String UPLOADING_PREFIX = "upload:uploading:";
    String MERGE_LOCK_PREFIX = "lock:merge:";
}
