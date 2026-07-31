package com.cloud.backend.service.file.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.config.FileProperties;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.constant.RedisConstants;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.SecUploadResponse;
import com.cloud.backend.dto.file.UploadInitRequest;
import com.cloud.backend.dto.file.UploadInitResponse;
import com.cloud.backend.dto.file.UploadPolicyResponse;
import com.cloud.backend.dto.file.UploadProgressResponse;
import com.cloud.backend.dto.file.UploadSecRequest;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.FileHash;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.service.file.FileHashService;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.file.UploadService;
import com.cloud.backend.service.user.UserService;
import com.cloud.backend.utils.FileUtil;
import com.cloud.backend.utils.IdUtil;
import com.cloud.backend.websocket.ProgressWebSocketHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 上传服务实现（分片上传 + 秒传 + 断点续传）。
 */
@Service
public class UploadServiceImpl implements UploadService {

    private final StringRedisTemplate redis;
    private final FileProperties fileProperties;
    private final FileMapper fileMapper;
    private final FileHashMapper fileHashMapper;
    private final FileHashService fileHashService;
    private final StorageService storageService;
    private final UserService userService;
    private final FileService fileService;
    private final ProgressWebSocketHandler progressHandler;
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;

    public UploadServiceImpl(StringRedisTemplate redis, FileProperties fileProperties, FileMapper fileMapper,
                             FileHashMapper fileHashMapper, FileHashService fileHashService,
                             StorageService storageService, UserService userService,
                             FileService fileService, ProgressWebSocketHandler progressHandler,
                             com.cloud.backend.service.admin.AdminSettingsService adminSettingsService) {
        this.redis = redis;
        this.fileProperties = fileProperties;
        this.fileMapper = fileMapper;
        this.fileHashMapper = fileHashMapper;
        this.fileHashService = fileHashService;
        this.storageService = storageService;
        this.userService = userService;
        this.fileService = fileService;
        this.progressHandler = progressHandler;
        this.adminSettingsService = adminSettingsService;
    }

    /* ==================== init ==================== */

    @Override
    public UploadPolicyResponse policy(Long userId) {
        boolean vip = isVip(userId);
        UploadPolicyResponse response = new UploadPolicyResponse();
        response.setMaxSize(vip ? adminSettingsService.getMaxSizeVip() : adminSettingsService.getMaxSizeUser());
        response.setMaxConcurrent(vip ? adminSettingsService.getMaxConcurrentVip() : adminSettingsService.getMaxConcurrentUser());
        return response;
    }

    @Override
    public UploadInitResponse init(Long userId, UploadInitRequest request) {
        String fileName = request.getFileName().trim();
        if (fileName.isEmpty() || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "文件名长度需在 1-255 之间");
        }
        validateParent(userId, request.getParentId());
        long fileSize = request.getFileSize();
        if (fileSize <= 0) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "文件大小必须大于 0");
        }
        // 配额校验
        long remaining = userService.getRemainingQuota(userId);
        if (fileSize > remaining) {
            throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED);
        }
        // 单文件大小上限（管理员配置，VIP 差异化）
        long maxSize = isVip(userId) ? adminSettingsService.getMaxSizeVip() : adminSettingsService.getMaxSizeUser();
        if (maxSize > 0 && fileSize > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        // 上传并发任务数（管理员配置，VIP 差异化）
        String uploadId = IdUtil.simpleUUID();
        checkConcurrentTasks(userId, uploadId);

        // 自适应分片：小文件单分片直传，大文件按配置分片
        long chunkSize = fileSize <= fileProperties.getSmallFileThreshold()
                ? Math.max(1, fileSize)
                : fileProperties.getChunkSize();
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        Map<String, String> meta = new java.util.HashMap<>();
        meta.put("userId", String.valueOf(userId));
        meta.put("fileName", fileName);
        meta.put("fileSize", String.valueOf(fileSize));
        meta.put("fileHash", request.getFileHash());
        meta.put("parentId", String.valueOf(request.getParentId()));
        meta.put("chunkSize", String.valueOf(chunkSize));
        meta.put("chunkCount", String.valueOf(totalChunks));
        meta.put("teamId", "0");
        Duration ttl = Duration.ofHours(fileProperties.getUploadExpireHours());
        redis.opsForHash().putAll(RedisConstants.UPLOAD_META_PREFIX + uploadId, meta);
        redis.expire(RedisConstants.UPLOAD_META_PREFIX + uploadId, ttl);
        redis.expire(RedisConstants.UPLOAD_CHUNKS_PREFIX + uploadId, ttl);
        redis.expire(RedisConstants.UPLOADING_PREFIX + userId, ttl);

        UploadInitResponse response = new UploadInitResponse();
        response.setUploadId(uploadId);
        response.setChunkSize(chunkSize);
        response.setTotalChunks(totalChunks);
        return response;
    }

    private void checkConcurrentTasks(Long userId, String uploadId) {
        String key = RedisConstants.UPLOADING_PREFIX + userId;
        redis.opsForSet().add(key, uploadId);
        Long count = redis.opsForSet().size(key);
        int limit = isVip(userId) ? adminSettingsService.getMaxConcurrentVip() : adminSettingsService.getMaxConcurrentUser();
        if (limit > 0 && count != null && count > limit) {
            redis.opsForSet().remove(key, uploadId);
            throw new BusinessException(ErrorCode.UPLOAD_TASK_EXCEEDED);
        }
    }

    /* ==================== chunk ==================== */

    @Override
    public void uploadChunk(Long userId, String uploadId, int seq, MultipartFile file) {
        Map<Object, Object> meta = getMeta(userId, uploadId);
        int chunkCount = Integer.parseInt((String) meta.get("chunkCount"));
        long chunkSize = Long.parseLong((String) meta.get("chunkSize"));
        if (seq < 1 || seq > chunkCount) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "分片序号非法");
        }
        if (file.getSize() > chunkSize) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "分片大小超过限制");
        }
        String chunksKey = RedisConstants.UPLOAD_CHUNKS_PREFIX + uploadId;
        String objectName = IdUtil.uploadChunkObject(userId, uploadId, seq);
        // 断点续传幂等：已传且对象存在则跳过
        if (Boolean.TRUE.equals(redis.opsForSet().isMember(chunksKey, String.valueOf(seq)))
                && storageService.objectExists(objectName)) {
            return;
        }
        redis.opsForSet().remove(chunksKey, String.valueOf(seq));
        try (InputStream inputStream = file.getInputStream()) {
            storageService.upload(objectName, inputStream, file.getSize(), "application/octet-stream");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "分片读取失败");
        }
        redis.opsForSet().add(chunksKey, String.valueOf(seq));
        // WebSocket 推送上传进度
        Long uploaded = redis.opsForSet().size(chunksKey);
        progressHandler.broadcast("upload", Map.of(
                "uploadId", uploadId,
                "uploaded", uploaded == null ? 0 : uploaded,
                "total", chunkCount));
    }

    /* ==================== progress ==================== */

    @Override
    public UploadProgressResponse progress(Long userId, String uploadId) {
        Map<Object, Object> meta = getMeta(userId, uploadId);
        Set<String> chunks = redis.opsForSet().members(RedisConstants.UPLOAD_CHUNKS_PREFIX + uploadId);
        List<Integer> uploadedChunks = new ArrayList<>();
        if (chunks != null) {
            chunks.stream().map(Integer::valueOf).sorted().forEach(uploadedChunks::add);
        }
        UploadProgressResponse response = new UploadProgressResponse();
        response.setUploadId(uploadId);
        response.setFileName((String) meta.get("fileName"));
        response.setFileSize(Long.parseLong((String) meta.get("fileSize")));
        response.setMimeType(FileUtil.getMimeType(FileUtil.getExtension((String) meta.get("fileName"))));
        response.setUploadedChunks(uploadedChunks);
        response.setParentId(Long.parseLong((String) meta.get("parentId")));
        response.setTeamId(Long.parseLong((String) meta.get("teamId")));
        return response;
    }

    /* ==================== merge ==================== */

    @Override
    @Log(operation = OperationType.UPLOAD_FILE, target = TargetType.FILE, targetId = "#result.id",
         detail = "'上传文件: ' + #result.name")
    public FileNodeResponse merge(Long userId, String uploadId) {
        Map<Object, Object> meta = getMeta(userId, uploadId);
        String fileName = (String) meta.get("fileName");
        long fileSize = Long.parseLong((String) meta.get("fileSize"));
        String fileHash = (String) meta.get("fileHash");
        long parentId = Long.parseLong((String) meta.get("parentId"));
        int chunkCount = Integer.parseInt((String) meta.get("chunkCount"));

        // 合并分布式锁，防并发合并
        String lockKey = RedisConstants.MERGE_LOCK_PREFIX + uploadId;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.UPLOAD_ALREADY_MERGED, "合并进行中或已完成");
        }
        boolean success = false;
        try {
            // 分片完整性校验
            Set<String> chunks = redis.opsForSet().members(RedisConstants.UPLOAD_CHUNKS_PREFIX + uploadId);
            if (chunks == null || chunks.size() != chunkCount) {
                List<Integer> missing = new ArrayList<>();
                for (int seq = 1; seq <= chunkCount; seq++) {
                    if (chunks == null || !chunks.contains(String.valueOf(seq))) {
                        missing.add(seq);
                    }
                }
                throw new BusinessException(ErrorCode.UPLOAD_CHUNK_MISSING, "缺失分片: " + missing);
            }

            // 插入占位记录拿 fileId（对象路径含 fileId）
            File file = buildFileRecord(userId, fileName, fileSize, fileHash, parentId);
            fileMapper.insert(file);
            String objectName = IdUtil.fileObject(userId, file.getId(), fileName);

            try {
                // 组合分片流 + 边传边算 SHA256
                List<InputStream> chunkStreams = new ArrayList<>();
                try {
                    for (int seq = 1; seq <= chunkCount; seq++) {
                        chunkStreams.add(storageService.download(
                                IdUtil.uploadChunkObject(userId, uploadId, seq)));
                    }
                    java.util.Enumeration<InputStream> enumeration = java.util.Collections.enumeration(chunkStreams);
                    InputStream merged = new java.io.SequenceInputStream(enumeration);
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    InputStream digestStream = new java.security.DigestInputStream(merged, digest);
                    String mimeType = FileUtil.getMimeType(FileUtil.getExtension(fileName));
                    storageService.upload(objectName, digestStream, fileSize, mimeType);
                    String actualHash = HexFormat.of().formatHex(digest.digest());
                    if (!actualHash.equalsIgnoreCase(fileHash)) {
                        storageService.delete(objectName);
                        fileMapper.deleteById(file.getId());
                        throw new BusinessException(ErrorCode.UPLOAD_INVALID, "文件哈希校验失败，请重新上传");
                    }
                } catch (BusinessException e) {
                    throw e;
                } catch (Exception e) {
                    storageService.delete(objectName);
                    fileMapper.deleteById(file.getId());
                    throw new BusinessException(ErrorCode.UPLOAD_MERGE_FAILED, "分片合并失败: " + e.getMessage());
                } finally {
                    for (InputStream stream : chunkStreams) {
                        try {
                            stream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

                // 秒传索引注册：并发已存在则复用共享对象
                String finalObjectName = fileHashService.register(fileHash, objectName, fileSize,
                        FileUtil.getMimeType(FileUtil.getExtension(fileName)));
                if (!finalObjectName.equals(objectName)) {
                    storageService.delete(objectName);
                    file.setObjectName(finalObjectName);
                } else {
                    file.setObjectName(objectName);
                }
                fileMapper.update(file);

                // 配额原子扣减（merge 完成一次性扣）
                userService.changeUsedSpace(userId, fileSize);
                success = true;
                return FileNodeResponse.from(file);
            } catch (RuntimeException e) {
                // 兜底补偿：删占位记录
                fileMapper.deleteById(file.getId());
                throw e;
            }
        } finally {
            // 锁始终释放；成功才清理上传上下文（失败保留分片与元数据，支持断点续传）
            redis.delete(RedisConstants.MERGE_LOCK_PREFIX + uploadId);
            if (success) {
                cleanupAfterMerge(userId, uploadId);
            }
        }
    }

    private void cleanupAfterMerge(Long userId, String uploadId) {
        try {
            redis.delete(RedisConstants.UPLOAD_META_PREFIX + uploadId);
            redis.delete(RedisConstants.UPLOAD_CHUNKS_PREFIX + uploadId);
            redis.opsForSet().remove(RedisConstants.UPLOADING_PREFIX + userId, uploadId);
            redis.delete(RedisConstants.MERGE_LOCK_PREFIX + uploadId);
        } catch (RuntimeException ignored) {
        }
    }

    /* ==================== sec（秒传） ==================== */

    @Override
    public SecUploadResponse sec(Long userId, UploadSecRequest request) {
        String fileName = request.getFileName().trim();
        if (fileName.isEmpty() || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "文件名长度需在 1-255 之间");
        }
        validateParent(userId, request.getParentId());
        FileHash hash = fileHashMapper.findByHash(request.getFileHash());
        if (hash == null) {
            return SecUploadResponse.miss();
        }
        if (!hash.getSize().equals(request.getFileSize())) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "文件大小与秒传索引不一致");
        }
        // 配额校验
        long remaining = userService.getRemainingQuota(userId);
        if (request.getFileSize() > remaining) {
            throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED);
        }
        // 共享引用 +1 + 新增记录
        String uniqueName = fileService.resolveUniqueName(userId, request.getParentId(), fileName);
        File file = buildFileRecord(userId, uniqueName, request.getFileSize(), request.getFileHash(),
                request.getParentId());
        file.setObjectName(hash.getObjectName());
        fileMapper.insert(file);
        fileHashService.shareRef(request.getFileHash());
        userService.changeUsedSpace(userId, request.getFileSize());
        return SecUploadResponse.hit(FileNodeResponse.from(file));
    }

    /* ==================== helpers ==================== */

    private Map<Object, Object> getMeta(Long userId, String uploadId) {
        Map<Object, Object> meta = redis.opsForHash().entries(RedisConstants.UPLOAD_META_PREFIX + uploadId);
        if (meta.isEmpty()) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_FOUND);
        }
        if (!String.valueOf(userId).equals(meta.get("userId"))) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_FOUND);
        }
        return meta;
    }

    private void validateParent(Long userId, Long parentId) {
        if (parentId == null || parentId == FileConstants.ROOT_PARENT_ID) {
            return;
        }
        File parent = fileService.getOwnedFile(userId, parentId);
        if (!parent.isDir()) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID, "父目录不存在");
        }
    }

    private boolean isVip(Long userId) {
        com.cloud.backend.entity.User user = userService.findById(userId);
        return Boolean.TRUE.equals(user.getIsVip());
    }

    private File buildFileRecord(Long userId, String fileName, long fileSize, String fileHash, Long parentId) {
        String extension = FileUtil.getExtension(fileName);
        File file = new File();
        file.setUserId(userId);
        file.setTeamId(0L);
        file.setParentId(parentId);
        file.setName(fileName);
        file.setPath(buildPath(userId, parentId, fileName));
        file.setSize(fileSize);
        file.setMimeType(FileUtil.getMimeType(extension));
        file.setExtension(extension);
        file.setFileHash(fileHash);
        file.setIsDirectory(0);
        file.setType(FileType.FILE);
        file.setCategory(FileUtil.categoryOf(extension));
        file.setObjectName("");
        file.setStatus(FileStatus.NORMAL);
        return file;
    }

    private String buildPath(Long userId, Long parentId, String name) {
        if (parentId == null || parentId == FileConstants.ROOT_PARENT_ID) {
            return "/" + name;
        }
        File parent = fileMapper.findById(parentId);
        if (parent == null || parent.getPath() == null) {
            return "/" + name;
        }
        return parent.getPath() + "/" + name;
    }
}
