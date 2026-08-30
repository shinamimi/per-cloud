package com.cloud.backend.constant;

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
