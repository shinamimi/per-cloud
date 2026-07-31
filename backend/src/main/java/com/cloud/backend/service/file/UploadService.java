package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.SecUploadResponse;
import com.cloud.backend.dto.file.UploadInitRequest;
import com.cloud.backend.dto.file.UploadInitResponse;
import com.cloud.backend.dto.file.UploadPolicyResponse;
import com.cloud.backend.dto.file.UploadProgressResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传服务 —— init / chunk / merge / sec（秒传）/ progress（断点续传）。
 *
 * 设计思路（file-module.md 第四节）：
 * - 自适应分片：小文件（阈值以下）单分片直传，大文件按 chunkSize 分片
 * - 分片状态与元数据存 Redis，支持断点续传查询
 * - merge 用 Redis 分布式锁（lock:merge:{uploadId}）防并发合并
 * - 配额在 merge 完成后一次性原子扣减（分片阶段不扣）
 * - 秒传：全站 SHA256 命中则引用现有对象，引用计数 +1
 */
public interface UploadService {

    UploadInitResponse init(Long userId, UploadInitRequest request);

    UploadPolicyResponse policy(Long userId);

    void uploadChunk(Long userId, String uploadId, int seq, MultipartFile file);

    UploadProgressResponse progress(Long userId, String uploadId);

    FileNodeResponse merge(Long userId, String uploadId);

    SecUploadResponse sec(Long userId, com.cloud.backend.dto.file.UploadSecRequest request);
}
