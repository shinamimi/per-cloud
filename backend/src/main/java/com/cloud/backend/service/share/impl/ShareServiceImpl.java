package com.cloud.backend.service.share.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.constant.RedisConstants;
import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.file.UploadSecRequest;
import com.cloud.backend.dto.share.*;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.Share;
import com.cloud.backend.entity.ShareFile;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.ShareStatus;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.DisabledObjectMapper;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.ShareFileMapper;
import com.cloud.backend.mapper.ShareMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.file.DownloadService;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.PreviewService;
import com.cloud.backend.service.file.UploadService;
import com.cloud.backend.service.share.ShareService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.util.ShareTokenGenerator;
import com.cloud.backend.utils.IpUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShareServiceImpl implements ShareService {

    /** 提取码错误锁定阈值（超限后拒绝验证，计数 TTL 过期后自动解锁） */
    private static final int PASSWORD_FAIL_LIMIT = 5;
    /** 提取码验证通过标记有效期（24 小时） */
    private static final Duration PASSWORD_OK_TTL = Duration.ofHours(24);

    private final ShareMapper shareMapper;
    private final ShareFileMapper shareFileMapper;
    private final FileMapper fileMapper;
    private final DisabledObjectMapper disabledObjectMapper;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final UploadService uploadService;
    private final DownloadService downloadService;
    private final PreviewService previewService;
    private final AdminSettingsService adminSettingsService;
    private final OperationLogService operationLogService;
    private final StringRedisTemplate redis;

    public ShareServiceImpl(ShareMapper shareMapper, ShareFileMapper shareFileMapper, FileMapper fileMapper,
                            DisabledObjectMapper disabledObjectMapper, UserMapper userMapper,
                            FileService fileService, UploadService uploadService, DownloadService downloadService,
                            PreviewService previewService, AdminSettingsService adminSettingsService,
                            OperationLogService operationLogService, StringRedisTemplate redis) {
        this.shareMapper = shareMapper;
        this.shareFileMapper = shareFileMapper;
        this.fileMapper = fileMapper;
        this.disabledObjectMapper = disabledObjectMapper;
        this.userMapper = userMapper;
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.downloadService = downloadService;
        this.previewService = previewService;
        this.adminSettingsService = adminSettingsService;
        this.operationLogService = operationLogService;
        this.redis = redis;
    }

    /* ==================== 基础 CRUD ==================== */

    /** 新增分享记录（基础 CRUD，业务创建走 createShare）。 */
    @Override
    public Share create(Share share) {
        shareMapper.insert(share);
        return share;
    }

    /** 按分享 token 查询记录。 */
    @Override
    public Share findByToken(String shareToken) {
        return shareMapper.findByToken(shareToken);
    }

    /** 按 id 查询分享记录。 */
    @Override
    public Share findById(Long id) {
        return shareMapper.findById(id);
    }

    /** 查询某用户创建的全部分享。 */
    @Override
    public List<Share> listByUserId(Long userId) {
        return shareMapper.findByUserId(userId);
    }

    /** 更新分享记录，返回受影响行数。 */
    @Override
    public int update(Share share) {
        return shareMapper.update(share);
    }

    /** 物理删除分享记录（基础 CRUD，业务删除走 deleteShareRecord）。 */
    @Override
    public int removeById(Long id) {
        return shareMapper.deleteById(id);
    }

    /** 查询全部分享记录（管理端用）。 */
    @Override
    public List<Share> findAll() {
        return shareMapper.findAll();
    }

    /**
     * 管理员取消分享：状态置 CANCELED、清除快照与访客提取码验证标记。
     * 权限约束：仅 ADMIN+ 可调。
     */
    @Override
    @Log(operation = OperationType.CANCEL_SHARE, target = TargetType.SHARE,
         targetId = "#id", detail = "'管理员取消分享'")
    public void adminCancelShare(Long id) {
        Share share = shareMapper.findById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        share.setStatus(ShareStatus.CANCELED);
        shareMapper.update(share);
        shareFileMapper.deleteByShareId(share.getId());
        clearVerified(share.getShareToken());
    }

    /** 管理员切换分享下载开关（仅改标记位）。权限约束：仅 ADMIN+ 可调。 */
    @Override
    @Log(operation = OperationType.UPDATE_USER, target = TargetType.SHARE,
         targetId = "#id", detail = "'管理员切换分享下载开关'")
    public void adminSetAllowDownload(Long id, boolean allowDownload) {
        Share share = shareMapper.findById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        shareMapper.updateAllowDownload(id, allowDownload ? 1 : 0);
    }

    /** 管理员删除分享记录（事务）：清除快照与验证标记后物理删除。权限约束：仅 ADMIN+ 可调。 */
    @Override
    @Log(operation = OperationType.DELETE_SHARE, target = TargetType.SHARE,
         targetId = "#id", detail = "'管理员删除分享记录'")
    @Transactional
    public void adminDeleteShare(Long id) {
        Share share = shareMapper.findById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        shareFileMapper.deleteByShareId(share.getId());
        shareMapper.deleteById(share.getId());
        clearVerified(share.getShareToken());
    }

    /* ==================== 用户侧分享管理 ==================== */

    /**
     * 创建分享（事务 + 写操作日志）：校验文件归属与禁用状态、每文件分享数上限，
     * 按请求组装有效期/提取码/下载策略/转存开关；目录分享时锁定当前文件树为快照。
     */
    @Override
    @Log(operation = OperationType.CREATE_SHARE, target = TargetType.SHARE,
         targetId = "#result.id", detail = "'创建分享'")
    @Transactional
    public Share createShare(Long userId, ShareCreateRequest request) {
        if (request == null || request.getFileId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要分享的文件");
        }
        File file = fileService.getOwnedFile(userId, request.getFileId());
        if (file.getStatus() != FileStatus.NORMAL) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        requireNotBlocked(file);

        int maxCount = adminSettingsService.getShareMaxCountPerFile();
        if (maxCount > 0 && shareMapper.countActiveByFileId(file.getId()) >= maxCount) {
            throw new BusinessException(ErrorCode.SHARE_COUNT_LIMIT);
        }

        Share share = new Share();
        share.setUserId(userId);
        share.setFileId(file.getId());
        share.setIsDir(file.isDir() ? 1 : 0);
        share.setShareToken(ShareTokenGenerator.generateUniqueToken(shareMapper));
        share.setStatus(ShareStatus.NORMAL);
        share.setDownloadCount(0);
        share.setMaxDownload(0);

        // 有效期：PERMANENT=永久（expire_time NULL）/ DAYS=按天数（上限 share.max-valid-days）
        boolean permanent = "PERMANENT".equalsIgnoreCase(request.getValidType());
        if (!permanent) {
            int days = request.getValidDays() != null && request.getValidDays() > 0
                    ? request.getValidDays() : adminSettingsService.getShareDefaultValidDays();
            int maxValidDays = adminSettingsService.getShareMaxValidDays();
            if (maxValidDays > 0 && days > maxValidDays) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "分享有效期不能超过 " + maxValidDays + " 天");
            }
            share.setExpireTime(LocalDateTime.now().plusDays(days));
        }

        // 提取码（默认值来自 share.default-require-password）
        boolean requirePassword = request.getRequirePassword() == null
                ? adminSettingsService.isShareDefaultRequirePassword() : request.getRequirePassword();
        if (requirePassword) {
            String password = request.getAccessPassword() == null ? "" : request.getAccessPassword().trim();
            if (password.isEmpty()) {
                throw new BusinessException(ErrorCode.SHARE_PASSWORD_EMPTY);
            }
            if (password.length() > 64) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "提取码不能超过 64 位");
            }
            share.setAccessPassword(password);
        } else {
            share.setAccessPassword("");
        }

        // 下载策略（默认值来自 share.default-download-policy）：允许下载可选次数限制 / 禁止下载
        boolean allowDownload = request.getAllowDownload() == null
                ? defaultAllowDownload() : request.getAllowDownload();
        share.setAllowDownload(allowDownload ? 1 : 0);
        if (allowDownload && request.getMaxDownload() != null && request.getMaxDownload() > 0) {
            share.setMaxDownload(request.getMaxDownload());
        }

        // 转存开关
        share.setAllowSave(request.getAllowSave() == null || request.getAllowSave() ? 1 : 0);

        shareMapper.insert(share);

        if (share.getIsDir() == 1) {
            snapshotSubtree(share, file);
        } else {
            shareFileMapper.insert(toSnapshot(share.getId(), 0L, file));
        }
        return share;
    }

    /** 查询本人分享列表，附当前原文件名（原文件已删除时显示占位文案）。 */
    @Override
    public List<ShareResponse> listShares(Long userId) {
        List<Share> shares = shareMapper.findByUserId(userId);
        if (shares.isEmpty()) {
            return List.of();
        }
        // 批量加载文件（替代 N+1）
        Set<Long> fileIds = shares.stream().map(Share::getFileId).collect(Collectors.toSet());
        Map<Long, File> fileMap = fileMapper.findByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
        return shares.stream().map(share -> {
            ShareResponse response = ShareResponse.from(share);
            File file = fileMap.get(share.getFileId());
            response.setName(file != null ? file.getName() : "（文件已删除）");
            return response;
        }).toList();
    }

    /** 修改分享有效期（仅生效中的分享可改）：永久或按天数续期，天数上限受管理员配置约束。 */
    @Override
    public void updateExpire(Long userId, Long shareId, ShareUpdateRequest request) {
        Share share = requireOwnedShare(userId, shareId);
        if (share.getStatus() != ShareStatus.NORMAL) {
            throw new BusinessException(ErrorCode.SHARE_CANCELED, "非生效中的分享不可修改有效期");
        }
        boolean permanent = "PERMANENT".equalsIgnoreCase(request.getValidType());
        if (permanent) {
            share.setExpireTime(null);
        } else {
            int days = request.getValidDays() != null && request.getValidDays() > 0
                    ? request.getValidDays() : adminSettingsService.getShareDefaultValidDays();
            int maxValidDays = adminSettingsService.getShareMaxValidDays();
            if (maxValidDays > 0 && days > maxValidDays) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "分享有效期不能超过 " + maxValidDays + " 天");
            }
            share.setExpireTime(LocalDateTime.now().plusDays(days));
        }
        shareMapper.update(share);
    }

    /** 用户取消分享（事务 + 写操作日志）：置 CANCELED、清快照与验证标记。 */
    @Override
    @Log(operation = OperationType.CANCEL_SHARE, target = TargetType.SHARE,
         targetId = "#shareId", detail = "'取消分享'")
    @Transactional
    public void cancelShare(Long userId, Long shareId) {
        Share share = requireOwnedShare(userId, shareId);
        share.setStatus(ShareStatus.CANCELED);
        shareMapper.update(share);
        shareFileMapper.deleteByShareId(share.getId());
        clearVerified(share.getShareToken());
    }

    /** 用户删除分享记录（事务 + 写操作日志）：清快照与验证标记后物理删除。 */
    @Override
    @Log(operation = OperationType.DELETE_SHARE, target = TargetType.SHARE,
         targetId = "#shareId", detail = "'删除分享记录'")
    @Transactional
    public void deleteShareRecord(Long userId, Long shareId) {
        Share share = requireOwnedShare(userId, shareId);
        shareFileMapper.deleteByShareId(share.getId());
        shareMapper.deleteById(share.getId());
        clearVerified(share.getShareToken());
    }

    /* ==================== 访客访问 ==================== */

    /** 访客获取分享访问信息：仅公开基础信息（名称/归属/状态/开关），不校验提取码。 */
    @Override
    public GuestShareInfoResponse getAccessInfo(String token) {
        Share share = requireAccessible(token);
        GuestShareInfoResponse response = new GuestShareInfoResponse();
        response.setShareToken(share.getShareToken());
        response.setIsDir(share.getIsDir() != null && share.getIsDir() == 1);
        com.cloud.backend.entity.User owner = userMapper.findById(share.getUserId());
        response.setOwnerName(owner != null ? owner.getNickname() : null);
        File file = fileMapper.findById(share.getFileId());
        response.setName(file != null ? file.getName() : "（文件已删除）");
        response.setStatus(share.getStatus());
        response.setRequirePassword(share.getAccessPassword() != null && !share.getAccessPassword().isEmpty());
        response.setAllowDownload(share.getAllowDownload() == null || share.getAllowDownload() == 1);
        response.setAllowSave(share.getAllowSave() == null || share.getAllowSave() == 1);
        response.setMaxDownload(share.getMaxDownload());
        response.setDownloadCount(share.getDownloadCount());
        if (response.getIsDir()) {
            response.setFileCount((int) shareFileMapper.findByShareId(share.getId()).stream()
                    .filter(n -> n.getIsDir() == 0).count());
        }
        return response;
    }

    /**
     * 提取码校验：无提取码直接放行并打验证标记；错误计数超限锁定（Redis 30 分钟计数），
     * 正确则清除失败计数并打验证标记（24 小时有效）。
     */
    @Override
    public void verifyPassword(String token, String password) {
        Share share = requireAccessible(token);
        if (share.getAccessPassword() == null || share.getAccessPassword().isEmpty()) {
            markVerified(token);
            return;
        }
        String failKey = RedisConstants.SHARE_PWD_FAIL_PREFIX + token;
        String fails = redis.opsForValue().get(failKey);
        if (fails != null && Integer.parseInt(fails) >= PASSWORD_FAIL_LIMIT) {
            throw new BusinessException(ErrorCode.SHARE_PASSWORD_LOCKED);
        }
        if (password == null || !share.getAccessPassword().equals(password.trim())) {
            long count = redis.opsForValue().increment(failKey);
            if (count == 1) {
                redis.expire(failKey, Duration.ofMinutes(30));
            }
            if (count >= PASSWORD_FAIL_LIMIT) {
                throw new BusinessException(ErrorCode.SHARE_PASSWORD_LOCKED);
            }
            throw new BusinessException(ErrorCode.SHARE_PASSWORD_INVALID,
                    "提取码错误，还可输入 " + (PASSWORD_FAIL_LIMIT - count) + " 次");
        }
        redis.delete(failKey);
        markVerified(token);
    }

    /** 访客获取分享快照文件列表（需已通过提取码验证）。 */
    @Override
    public List<ShareFileNodeResponse> getShareFiles(String token) {
        Share share = requireAccessible(token);
        requirePasswordVerified(share);
        return shareFileMapper.findByShareId(share.getId()).stream().map(node -> {
            ShareFileNodeResponse response = new ShareFileNodeResponse();
            response.setId(node.getId());
            response.setParentId(node.getParentId());
            response.setName(node.getName());
            response.setIsDir(node.getIsDir() != null && node.getIsDir() == 1);
            response.setSize(node.getSize());
            response.setMimeType(node.getMimeType());
            response.setExtension(node.getExtension());
            return response;
        }).toList();
    }

    /**
     * 访客获取单文件下载 URL：需开启下载 + 通过提取码验证，快照节点须在分享内，
     * 回查原文件可用后计数（同 IP 60 秒去重）并生成预签名 URL。
     */
    @Override
    public String getShareDownloadUrl(String token, Long snapshotId) {
        Share share = requireAccessible(token);
        requireDownloadAllowed(share);
        requirePasswordVerified(share);
        ShareFile node = requireSnapshot(share.getId(), snapshotId);
        if (node.getIsDir() != null && node.getIsDir() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目录不可直接下载");
        }
        File file = requireShareableFile(share, node);
        countDownload(share, dedupKey(share.getId(), snapshotId));
        return downloadService.getDownloadUrlForShare(file);
    }

    /** 访客预览分享文件（需提取码验证；回查原文件可用性）。 */
    @Override
    public FilePreviewResponse previewShareFile(String token, Long snapshotId) {
        Share share = requireAccessible(token);
        requirePasswordVerified(share);
        ShareFile node = requireSnapshot(share.getId(), snapshotId);
        if (node.getIsDir() != null && node.getIsDir() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目录不可预览");
        }
        File file = requireShareableFile(share, node);
        return previewService.previewFile(share.getUserId(), file);
    }

    /** 访客批量打包下载（需开启下载 + 提取码验证）：选中目录递归展开为文件清单，计数后创建打包任务。 */
    @Override
    public BatchDownloadResponse batchDownload(String token, List<Long> snapshotIds) {
        Share share = requireAccessible(token);
        requireDownloadAllowed(share);
        requirePasswordVerified(share);
        List<File> files = collectShareFiles(share, snapshotIds);
        if (files.isEmpty()) {
            throw new BusinessException(ErrorCode.BATCH_TASK_NOT_FOUND, "没有可打包的文件");
        }
        countDownload(share, dedupKey(share.getId(), snapshotIds));
        return downloadService.createBatchTaskForGuest(files);
    }

    /** 查询分享批量打包任务状态（转发到下载服务）。 */
    @Override
    public BatchDownloadResponse getBatchTask(String taskId) {
        return downloadService.getBatchTask(taskId);
    }

    /**
     * 访客转存分享内容到本人空间（事务）：剔除嵌套选中项后按深度排序，
     * 先建目录树再逐文件秒传转存；原文件失效或命中对象级禁用则整体中止。
     */
    @Override
    @Transactional
    public void saveShareFiles(Long userId, String token, List<Long> snapshotIds) {
        Share share = requireAccessible(token);
        if (share.getAllowSave() == null || share.getAllowSave() != 1) {
            throw new BusinessException(ErrorCode.SHARE_SAVE_DISABLED);
        }
        requirePasswordVerified(share);
        if (snapshotIds == null || snapshotIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要转存的内容");
        }
        List<ShareFile> all = shareFileMapper.findByShareId(share.getId());
        Map<Long, ShareFile> byId = all.stream().collect(Collectors.toMap(ShareFile::getId, n -> n));
        List<ShareFile> selected = new ArrayList<>();
        for (Long id : snapshotIds) {
            ShareFile node = byId.get(id);
            if (node != null) {
                selected.add(node);
            }
        }
        if (selected.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "转存内容不存在");
        }
        // 剔除祖先也在选中集的节点（目录已包含其子树，避免重复转存）
        Set<Long> selectedIds = selected.stream().map(ShareFile::getId).collect(Collectors.toSet());
        selected = selected.stream()
                .filter(n -> !hasSelectedAncestor(n, byId, selectedIds))
                .sorted(Comparator.comparingInt(n -> snapshotDepth(n, byId)))
                .toList();

        // 1. 先建目录树（父先于子，快照目录 → 新目录 id 映射）
        Map<Long, Long> dirMap = new HashMap<>();
        for (ShareFile node : selected) {
            if (node.getIsDir() != null && node.getIsDir() == 1) {
                Long targetParent = dirMap.getOrDefault(node.getParentId(), FileConstants.ROOT_PARENT_ID);
                DirectoryCreateRequest req = new DirectoryCreateRequest();
                req.setName(node.getName());
                req.setParentId(targetParent);
                File dir = fileService.createDirectory(userId, req);
                dirMap.put(node.getId(), dir.getId());
            }
        }
        // 2. 文件逐个秒传转存（引用计数 +1，零复制）
        for (ShareFile node : selected) {
            if (node.getIsDir() != null && node.getIsDir() == 1) {
                continue;
            }
            File original = fileMapper.findById(node.getFileId());
            if (original == null || original.getStatus() != FileStatus.NORMAL) {
                throw new BusinessException(ErrorCode.SHARE_FILE_REMOVED, "部分分享内容已失效，转存中止");
            }
            if (disabledObjectMapper.countBlocked(node.getFileHash(), share.getUserId()) > 0) {
                throw new BusinessException(ErrorCode.FILE_DISABLED, "部分分享内容已被禁用，转存中止");
            }
            UploadSecRequest secRequest = new UploadSecRequest();
            secRequest.setFileHash(node.getFileHash());
            secRequest.setFileName(node.getName());
            secRequest.setFileSize(node.getSize());
            secRequest.setParentId(dirMap.getOrDefault(node.getParentId(), FileConstants.ROOT_PARENT_ID));
            uploadService.sec(userId, secRequest);
        }
    }

    /* ==================== 内部工具 ==================== */

    /** 校验分享可访问：存在、未取消/未达上限、未过期；任一不满足抛对应业务异常。 */
    private Share requireAccessible(String token) {
        Share share = shareMapper.findByToken(token);
        if (share == null) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        if (share.getStatus() == ShareStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SHARE_CANCELED);
        }
        if (share.getStatus() == ShareStatus.EXHAUSTED) {
            throw new BusinessException(ErrorCode.SHARE_EXHAUSTED);
        }
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SHARE_EXPIRED);
        }
        return share;
    }

    /** 取本人创建的分享（不存在或非本人所有抛业务异常）。 */
    private Share requireOwnedShare(Long userId, Long shareId) {
        Share share = shareMapper.findById(shareId);
        if (share == null || !share.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        return share;
    }

    /** 分享必须允许下载，否则抛业务异常。 */
    private void requireDownloadAllowed(Share share) {
        if (share.getAllowDownload() == null || share.getAllowDownload() != 1) {
            throw new BusinessException(ErrorCode.SHARE_DOWNLOAD_DISABLED);
        }
    }

    /** 有提取码的分享必须先通过验证（Redis 标记），否则要求输入提取码。 */
    private void requirePasswordVerified(Share share) {
        if (share.getAccessPassword() == null || share.getAccessPassword().isEmpty()) {
            return;
        }
        String ok = redis.opsForValue().get(RedisConstants.SHARE_PWD_OK_PREFIX + share.getShareToken());
        if (!"1".equals(ok)) {
            throw new BusinessException(ErrorCode.SHARE_PASSWORD_REQUIRED);
        }
    }

    /** 写入提取码验证通过标记（带有效期）。 */
    private void markVerified(String token) {
        redis.opsForValue().set(RedisConstants.SHARE_PWD_OK_PREFIX + token, "1", PASSWORD_OK_TTL);
    }

    /** 清除提取码验证通过/失败计数标记（分享被取消、删除时同步清理）。 */
    private void clearVerified(String token) {
        redis.delete(RedisConstants.SHARE_PWD_OK_PREFIX + token);
        redis.delete(RedisConstants.SHARE_PWD_FAIL_PREFIX + token);
    }

    /** 分享下载去重键：share:dl-dedup:{shareId}:{scope}:{clientIp}（scope=单文件 snapshotId 或批量标记） */
    private String dedupKey(Long shareId, Object scope) {
        String clientIp = currentClientIp();
        return RedisConstants.SHARE_DOWNLOAD_DEDUP_PREFIX + shareId + ":" + scope + ":" + clientIp;
    }

    /** 当前请求客户端 IP（无请求上下文时返回 "anon"） */
    private String currentClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "anon";
        }
        String ip = IpUtil.getClientIp(attrs.getRequest());
        return ip == null || ip.isBlank() ? "anon" : ip;
    }

    /** 快照节点必须在分享快照内（防跨分享越权） */
    private ShareFile requireSnapshot(Long shareId, Long snapshotId) {
        List<ShareFile> nodes = shareFileMapper.findByShareId(shareId);
        return nodes.stream().filter(n -> n.getId().equals(snapshotId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_NOT_FOUND, "分享内容不存在"));
    }

    /** 回查原文件：分享内容基于快照展示，但下载/预览前校验原文件存在且未被禁用（安全维度） */
    private File requireShareableFile(Share share, ShareFile node) {
        File file = fileMapper.findById(node.getFileId());
        if (file == null || file.getStatus() != FileStatus.NORMAL) {
            throw new BusinessException(ErrorCode.SHARE_FILE_REMOVED);
        }
        if (disabledObjectMapper.countBlocked(node.getFileHash(), share.getUserId()) > 0) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        return file;
    }

    /** 原子下载计数：仅生效中且未达上限才 +1；达限置 EXHAUSTED（并发下由 UPDATE 语句原子保证）。 */
    private void countDownload(Share share, String dedupKey) {
        // 短时间（60s）同一访客对同一文件重复下载只计 1 次，防止刷新/误点刷掉下载上限
        if (dedupKey != null) {
            Boolean first = redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofSeconds(60));
            if (!Boolean.TRUE.equals(first)) {
                return;
            }
        }
        if (shareMapper.incrementDownloadCountIfAllowed(share.getId()) > 0) {
            return;
        }
        Share latest = shareMapper.findById(share.getId());
        if (latest.getExpireTime() != null && latest.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SHARE_EXPIRED);
        }
        if (latest.getStatus() == ShareStatus.CANCELED) {
            throw new BusinessException(ErrorCode.SHARE_CANCELED);
        }
        if (latest.getMaxDownload() > 0 && latest.getDownloadCount() >= latest.getMaxDownload()) {
            if (latest.getStatus() == ShareStatus.NORMAL) {
                latest.setStatus(ShareStatus.EXHAUSTED);
                shareMapper.update(latest);
            }
            throw new BusinessException(ErrorCode.SHARE_EXHAUSTED);
        }
        throw new BusinessException(ErrorCode.SHARE_EXHAUSTED);
    }

    /** 从快照选中集收集可打包文件（目录递归展开；原文件失效的节点跳过） */
    private List<File> collectShareFiles(Share share, List<Long> snapshotIds) {
        List<ShareFile> all = shareFileMapper.findByShareId(share.getId());
        Map<Long, ShareFile> byId = all.stream().collect(Collectors.toMap(ShareFile::getId, n -> n));
        Set<Long> requested = snapshotIds == null ? Set.of() : new HashSet<>(snapshotIds);
        List<ShareFile> selected = new ArrayList<>();
        for (Long id : requested) {
            ShareFile node = byId.get(id);
            if (node != null) {
                selected.add(node);
            }
        }
        Set<Long> selectedIds = selected.stream().map(ShareFile::getId).collect(Collectors.toSet());
        selected = selected.stream()
                .filter(n -> !hasSelectedAncestor(n, byId, selectedIds))
                .toList();
        // BFS 展开选中目录为文件清单
        Map<Long, List<ShareFile>> childrenByParent = all.stream()
                .collect(Collectors.groupingBy(ShareFile::getParentId));
        List<ShareFile> fileNodes = new ArrayList<>();
        Deque<ShareFile> queue = new ArrayDeque<>(selected);
        while (!queue.isEmpty()) {
            ShareFile node = queue.poll();
            if (node.getIsDir() != null && node.getIsDir() == 1) {
                queue.addAll(childrenByParent.getOrDefault(node.getId(), List.of()));
                continue;
            }
            fileNodes.add(node);
        }
        if (fileNodes.isEmpty()) {
            return List.of();
        }
        // 批量加载原始文件（替代 N+1）
        Set<Long> originalFileIds = fileNodes.stream().map(ShareFile::getFileId).collect(Collectors.toSet());
        Map<Long, File> originalFileMap = fileMapper.findByIds(originalFileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
        List<File> files = new ArrayList<>();
        for (ShareFile node : fileNodes) {
            File original = originalFileMap.get(node.getFileId());
            if (original == null || original.getStatus() != FileStatus.NORMAL) {
                continue;
            }
            if (disabledObjectMapper.countBlocked(node.getFileHash(), share.getUserId()) > 0) {
                continue;
            }
            File zipFile = new File();
            zipFile.setName(node.getName());
            zipFile.setPath(buildSnapshotPath(node, byId));
            zipFile.setObjectName(original.getObjectName());
            zipFile.setSize(node.getSize());
            files.add(zipFile);
        }
        return files;
    }

    /** 快照相对路径（/dir/file），供 zip 条目保持目录结构 */
    private String buildSnapshotPath(ShareFile node, Map<Long, ShareFile> byId) {
        Deque<String> names = new ArrayDeque<>();
        names.push(node.getName());
        ShareFile cursor = node;
        while (cursor.getParentId() != null && cursor.getParentId() != 0 && byId.containsKey(cursor.getParentId())) {
            cursor = byId.get(cursor.getParentId());
            names.push(cursor.getName());
        }
        return "/" + String.join("/", names);
    }

    /** 快照节点是否存在已选中的祖先（用于剔除嵌套选中项，避免重复处理）。 */
    private boolean hasSelectedAncestor(ShareFile node, Map<Long, ShareFile> byId, Set<Long> selectedIds) {
        ShareFile cursor = node;
        while (cursor.getParentId() != null && cursor.getParentId() != 0 && byId.containsKey(cursor.getParentId())) {
            cursor = byId.get(cursor.getParentId());
            if (selectedIds.contains(cursor.getId())) {
                return true;
            }
        }
        return false;
    }

    /** 快照节点在快照树中的深度（转存时父先于子排序用）。 */
    private int snapshotDepth(ShareFile node, Map<Long, ShareFile> byId) {
        int depth = 0;
        ShareFile cursor = node;
        while (cursor.getParentId() != null && cursor.getParentId() != 0 && byId.containsKey(cursor.getParentId())) {
            cursor = byId.get(cursor.getParentId());
            depth++;
        }
        return depth;
    }

    /** 目录分享：BFS 锁定创建时的文件树到快照表（仅 NORMAL 文件；禁用文件不进快照） */
    private void snapshotSubtree(Share share, File root) {
        Map<Long, List<File>> childrenByParent = fileMapper.findByUserId(share.getUserId()).stream()
                .filter(f -> f.getStatus() == FileStatus.NORMAL)
                .collect(Collectors.groupingBy(File::getParentId));
        Map<Long, Long> snapshotIdByFileId = new HashMap<>();
        ShareFile rootNode = toSnapshot(share.getId(), 0L, root);
        shareFileMapper.insert(rootNode);
        snapshotIdByFileId.put(root.getId(), rootNode.getId());
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(root.getId());
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (File child : childrenByParent.getOrDefault(current, List.of())) {
                ShareFile node = toSnapshot(share.getId(), snapshotIdByFileId.getOrDefault(current, 0L), child);
                shareFileMapper.insert(node);
                snapshotIdByFileId.put(child.getId(), node.getId());
                if (child.isDir()) {
                    queue.add(child.getId());
                }
            }
        }
    }

    /** 把文件实体转为分享快照节点（拷贝名称/大小/MIME/hash 等展示与转存所需字段）。 */
    private ShareFile toSnapshot(Long shareId, Long parentId, File file) {
        ShareFile node = new ShareFile();
        node.setShareId(shareId);
        node.setFileId(file.getId());
        node.setParentId(parentId);
        node.setName(file.getName());
        node.setIsDir(file.isDir() ? 1 : 0);
        node.setSize(file.getSize());
        node.setMimeType(file.getMimeType());
        node.setExtension(file.getExtension());
        node.setFileHash(file.getFileHash());
        return node;
    }

    /** 分享的文件内容命中对象级禁用（全站禁或仅该用户禁）→ 拒绝创建分享。 */
    private void requireNotBlocked(File file) {
        if (file.getFileHash() != null && !file.getFileHash().isEmpty()
                && disabledObjectMapper.countBlocked(file.getFileHash(), file.getUserId()) > 0) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
    }

    /** 下载策略默认值：管理员策略非 DENY 即默认允许下载。 */
    private boolean defaultAllowDownload() {
        return !"DENY".equalsIgnoreCase(adminSettingsService.getShareDefaultDownloadPolicy());
    }
}
