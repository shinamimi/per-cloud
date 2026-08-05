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
 *
 * 修改指引：
 * - 【习惯】想改"分片策略（阈值/chunkSize/上传会话过期时间）" → init() 中 fileProperties.getSmallFileThreshold()/
 *   getChunkSize()/getUploadExpireHours()；改动影响分片数量、断点续传窗口与 Redis 占用
 * - 【习惯】想改"单文件大小上限/并发任务数（VIP 差异化限流）" → policy()/init() 中
 *   adminSettingsService 的 getMaxSizeUser/Vip、getMaxConcurrentUser/Vip 与 checkConcurrentTasks() 惰性清理；
 *   改动影响上传准入与限流
 * - 【习惯】想改"配额口径" → init()/merge()/sec() 中 teamId>0 扣团队配额、否则扣个人配额（配合 UserService/TeamService
 *   的 changeUsedSpace）；改动影响团队/个人配额一致性
 * - 【习惯】想改"并发合并控制" → merge() 中 Redis 分布式锁（RedisConstants.MERGE_LOCK_PREFIX）setIfAbsent 5 分钟；
 *   改动影响防并发重复合并语义
 * - 【习惯】想改"断点续传幂等" → uploadChunk() 中"已登记且对象已存在则跳过"；改动影响续传正确性
 * - 【习惯】想改"秒传命中/违规拦截" → sec()/merge() 的 requireNotBlocked()（对象级禁用全站/仅用户禁）与
 *   fileHashService.register()/shareRef()（引用 +1）；改动影响去重与违规上传拦截
 * - 【习惯】操作日志：merge() 用 @Log 切面（UPLOAD_FILE）；改动影响 OperationLogService
 * - 【习惯】并发注意：分片阶段不扣配额、merge/sec 完成才原子扣减，秒传引用原子 +1；改动勿引入非原子读改写
 * - 【习惯】新增方法 → 需同步实现类 UploadServiceImpl 及 FileController、ShareServiceImpl（转存复用 sec()）等调用方
 */
public interface UploadService {

    UploadInitResponse init(Long userId, UploadInitRequest request);

    UploadPolicyResponse policy(Long userId);

    void uploadChunk(Long userId, String uploadId, int seq, MultipartFile file);

    UploadProgressResponse progress(Long userId, String uploadId);

    FileNodeResponse merge(Long userId, String uploadId);

    SecUploadResponse sec(Long userId, com.cloud.backend.dto.file.UploadSecRequest request);
}
