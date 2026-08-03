package com.cloud.backend.service.team.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.dto.file.RecycleBinResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.enums.TeamMemberRole;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.DisabledObjectMapper;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.RecycleBinMapper;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.file.PreviewService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.team.TeamFileService;
import com.cloud.backend.service.team.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 团队文件服务实现 —— 团队目录树、文件管理、下载预览与团队回收站。
 *
 * 设计思路：
 * - 目录与文件复用个人文件的统一表模型，所有查询带 teamId 维度；
 *   团队成员共享同一命名空间（同团队同目录 name 唯一，跨 user_id）
 * - 删除进团队回收站（记录带 team_id），恢复/彻底删除的权限与文件管理权限一致
 * - 写操作（重命名/移动/复制/删除）按角色校验：ADMIN/OWNER 可操作团队所有文件，
 *   MEMBER 只能操作自己上传的文件
 * - 禁用/对象级禁用文件对成员不可下载，管理端不受此限
 */
@Service
public class TeamFileServiceImpl implements TeamFileService {

    private final FileMapper fileMapper;
    private final FileHashMapper fileHashMapper;
    private final RecycleBinMapper recycleBinMapper;
    private final StorageService storageService;
    private final TeamService teamService;
    private final PreviewService previewService;
    private final OperationLogService operationLogService;
    private final AdminSettingsService adminSettingsService;
    private final com.cloud.backend.service.file.RecycleBinService recycleBinService;
    private final com.cloud.backend.mapper.UserMapper userMapper;
    private final DisabledObjectMapper disabledObjectMapper;

    public TeamFileServiceImpl(FileMapper fileMapper, FileHashMapper fileHashMapper,
                               RecycleBinMapper recycleBinMapper, StorageService storageService,
                               TeamService teamService, PreviewService previewService,
                               OperationLogService operationLogService, AdminSettingsService adminSettingsService,
                               com.cloud.backend.service.file.RecycleBinService recycleBinService,
                               com.cloud.backend.mapper.UserMapper userMapper,
                               DisabledObjectMapper disabledObjectMapper) {
        this.fileMapper = fileMapper;
        this.fileHashMapper = fileHashMapper;
        this.recycleBinMapper = recycleBinMapper;
        this.storageService = storageService;
        this.teamService = teamService;
        this.previewService = previewService;
        this.operationLogService = operationLogService;
        this.adminSettingsService = adminSettingsService;
        this.recycleBinService = recycleBinService;
        this.userMapper = userMapper;
        this.disabledObjectMapper = disabledObjectMapper;
    }

    /* ==================== 列表 / 树 / 目录 ==================== */

    /** 分页查询团队某目录下的文件（调用方须为团队成员），补充上传者显示名。 */
    @Override
    public Page<FileNodeResponse> listFiles(Long teamId, Long userId, Long parentId, int page, int size) {
        teamService.requireMember(teamId, userId);
        long safeParent = parentId == null ? FileConstants.ROOT_PARENT_ID : parentId;
        if (safeParent != FileConstants.ROOT_PARENT_ID) {
            getTeamDirectory(teamId, safeParent);
        }
        int offset = (page - 1) * size;
        long total = fileMapper.countByTeamIdAndParentId(teamId, safeParent);
        List<FileNodeResponse> records = fileMapper.pageByTeamIdAndParentId(teamId, safeParent, offset, size)
                .stream().map(FileNodeResponse::from).toList();
        fillUploaders(records);
        return new Page<>(records, total, page, size);
    }

    /** 构建团队目录树（仅目录节点）。 */
    @Override
    public List<FileTreeResponse> tree(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        Map<Long, List<File>> childrenByParent = fileMapper.findByTeamId(teamId).stream()
                .filter(File::isDir)
                .collect(Collectors.groupingBy(File::getParentId));
        List<FileTreeResponse> roots = new ArrayList<>();
        for (File file : childrenByParent.getOrDefault(FileConstants.ROOT_PARENT_ID, List.of())) {
            roots.add(buildTree(file, childrenByParent));
        }
        return roots;
    }

    /** 递归构建单个目录节点及其子目录。 */
    private FileTreeResponse buildTree(File dir, Map<Long, List<File>> childrenByParent) {
        FileTreeResponse node = FileTreeResponse.of(dir.getId(), dir.getName(), true);
        for (File child : childrenByParent.getOrDefault(dir.getId(), List.of())) {
            node.getChildren().add(buildTree(child, childrenByParent));
        }
        return node;
    }

    /** 创建团队目录（写操作日志）：调用方须为团队成员；校验名称与父目录，同名自动加后缀。 */
    @Override
    @Log(operation = OperationType.CREATE_DIRECTORY, target = TargetType.FILE, targetId = "#request.parentId",
         detail = "'创建团队目录: ' + #request.name")
    public FileNodeResponse createDirectory(Long teamId, Long userId, DirectoryCreateRequest request) {
        teamService.requireMember(teamId, userId);
        String name = request.getName().trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目录名长度需在 1-255 之间");
        }
        if (request.getParentId() != FileConstants.ROOT_PARENT_ID) {
            getTeamDirectory(teamId, request.getParentId());
        }
        String uniqueName = resolveTeamUniqueName(teamId, request.getParentId(), name);
        File dir = new File();
        dir.setUserId(userId);
        dir.setTeamId(teamId);
        dir.setParentId(request.getParentId());
        dir.setName(uniqueName);
        dir.setPath(buildPath(request.getParentId(), uniqueName));
        dir.setSize(0L);
        dir.setIsDirectory(1);
        dir.setType(FileType.DIRECTORY);
        dir.setCategory(FileConstants.OTHER);
        dir.setObjectName("");
        dir.setStatus(FileStatus.NORMAL);
        fileMapper.insert(dir);
        return FileNodeResponse.from(dir);
    }

    /* ==================== 重命名 / 移动 / 复制 ==================== */

    /** 重命名团队文件/目录：须为成员且有写权限，目标名冲突自动加序号后缀。 */
    @Override
    public FileNodeResponse rename(Long teamId, Long userId, Long fileId, String name) {
        teamService.requireMember(teamId, userId);
        String newName = name.trim();
        if (newName.isEmpty() || newName.length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名长度需在 1-255 之间");
        }
        File file = getTeamFile(teamId, fileId);
        requireFileWritePermission(teamId, userId, file);
        String uniqueName = resolveTeamUniqueName(teamId, file.getParentId(), newName);
        fileMapper.updateName(fileId, uniqueName);
        file.setName(uniqueName);
        return FileNodeResponse.from(file);
    }

    /** 移动团队文件/目录：须为成员且有写权限，不能移入自身或其子目录，目标位置冲突自动加后缀。 */
    @Override
    public FileNodeResponse move(Long teamId, Long userId, Long fileId, Long targetParentId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        requireFileWritePermission(teamId, userId, file);
        long safeTarget = targetParentId == null ? FileConstants.ROOT_PARENT_ID : targetParentId;
        if (safeTarget != FileConstants.ROOT_PARENT_ID) {
            getTeamDirectory(teamId, safeTarget);
        }
        if (safeTarget == fileId) {
            throw new BusinessException(ErrorCode.MOVE_INVALID);
        }
        if (file.isDir()) {
            for (Long childId : collectSubtree(teamId, fileId).stream().map(File::getId).toList()) {
                if (safeTarget == childId) {
                    throw new BusinessException(ErrorCode.MOVE_INVALID);
                }
            }
        }
        String uniqueName = resolveTeamUniqueName(teamId, safeTarget, file.getName());
        fileMapper.updateParent(fileId, safeTarget);
        if (!uniqueName.equals(file.getName())) {
            fileMapper.updateName(fileId, uniqueName);
            file.setName(uniqueName);
        }
        file.setParentId(safeTarget);
        return FileNodeResponse.from(file);
    }

    /**
     * 复制团队文件/目录（事务）：目录递归复制，文件共享对象存储引用（引用计数 +1）；
     * 复制占用团队配额，配额不足整体回滚。须为成员且有写权限。
     */
    @Override
    @Transactional
    public FileNodeResponse copy(Long teamId, Long userId, Long fileId, Long targetParentId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        requireFileWritePermission(teamId, userId, file);
        long safeTarget = targetParentId == null ? FileConstants.ROOT_PARENT_ID : targetParentId;
        if (safeTarget != FileConstants.ROOT_PARENT_ID) {
            getTeamDirectory(teamId, safeTarget);
        }
        if (safeTarget == fileId) {
            throw new BusinessException(ErrorCode.MOVE_INVALID, "不能复制到自身");
        }
        Map<Long, Long> idMap = new HashMap<>();
        File copied = copyNode(teamId, userId, file, safeTarget, idMap);
        if (file.isDir()) {
            for (File child : collectSubtree(teamId, fileId)) {
                if (child.getId().equals(fileId)) {
                    continue;
                }
                Long newParent = child.getParentId().equals(fileId)
                        ? copied.getId()
                        : idMap.get(child.getParentId());
                if (newParent == null) {
                    newParent = safeTarget;
                }
                copyNode(teamId, userId, child, newParent, idMap);
            }
        }
        // 复制占用团队配额（等价于秒传）
        long extraSize = collectSubtree(teamId, copied.getId()).stream()
                .map(File::getSize).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).sum();
        if (extraSize > 0) {
            teamService.checkQuota(teamId, extraSize);
            teamService.changeUsedSpace(teamId, extraSize);
        }
        return FileNodeResponse.from(copied);
    }

    /** 复制单个节点：新增记录；文件共享对象引用（秒传引用计数 +1），父子映射关系记入 idMap。 */
    private File copyNode(Long teamId, Long userId, File source, Long targetParentId, Map<Long, Long> idMap) {
        String uniqueName = resolveTeamUniqueName(teamId, targetParentId, source.getName());
        File copy = new File();
        copy.setUserId(userId);
        copy.setTeamId(teamId);
        copy.setParentId(targetParentId);
        copy.setName(uniqueName);
        copy.setPath(buildPath(targetParentId, uniqueName));
        copy.setSize(source.getSize());
        copy.setMimeType(source.getMimeType());
        copy.setExtension(source.getExtension());
        copy.setFileHash(source.getFileHash());
        copy.setIsDirectory(source.getIsDirectory());
        copy.setType(source.getType());
        copy.setCategory(source.getCategory());
        copy.setObjectName(source.getObjectName());
        copy.setStatus(FileStatus.NORMAL);
        fileMapper.insert(copy);
        if (!source.isDir() && source.getFileHash() != null && !source.getFileHash().isEmpty()) {
            fileHashMapper.incrementRefCount(source.getFileHash());
        }
        idMap.put(source.getId(), copy.getId());
        return copy;
    }

    /* ==================== 删除（团队回收站） ==================== */

    /**
     * 删除到团队回收站（事务）：子树整体置 DELETED，逐节点写回收站记录并释放团队配额、
     * 写操作日志；顶层节点改内部名避免占用唯一索引。须为成员且有写权限。
     */
    @Override
    @Transactional
    public void deleteToRecycle(Long teamId, Long userId, Long fileId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        requireFileWritePermission(teamId, userId, file);
        // 顶层节点改内部名，避免占用唯一索引（同团队同目录 name 唯一）
        String originalName = file.getName();
        String tombstone = tombstoneName(originalName, fileId);
        fileMapper.updateName(fileId, tombstone);
        List<File> nodes = collectSubtree(teamId, fileId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(adminSettingsService.getTeamRecycleBinDays());
        long totalSize = 0;
        for (File node : nodes) {
            totalSize += node.getSize() != null ? node.getSize() : 0;
            fileMapper.updateStatus(node.getId(), FileStatus.DELETED.getValue());
            RecycleBin recycleBin = new RecycleBin();
            recycleBin.setUserId(node.getUserId());
            recycleBin.setFileId(node.getId());
            recycleBin.setOriginalName(node.getId().equals(fileId) ? originalName : node.getName());
            recycleBin.setObjectName(node.getObjectName() == null ? "" : node.getObjectName());
            recycleBin.setFileHash(node.getFileHash() == null ? "" : node.getFileHash());
            recycleBin.setType(node.isDir() ? 1 : 0);
            recycleBin.setTeamId(teamId);
            recycleBin.setDeletedBy(0);
            recycleBin.setParentId(node.getParentId());
            recycleBin.setSize(node.getSize() == null ? 0 : node.getSize());
            recycleBin.setMimeType(node.getMimeType() == null ? "" : node.getMimeType());
            recycleBin.setDeletedTime(now);
            recycleBin.setExpireTime(expireTime);
            recycleBinMapper.insert(recycleBin);
        }
        // 释放团队配额
        if (totalSize > 0) {
            teamService.changeUsedSpace(teamId, -totalSize);
        }
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.DELETE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(fileId);
        log.setDetail("删除团队文件/目录: " + file.getName()
                + (nodes.size() > 1 ? "（含 " + (nodes.size() - 1) + " 个子项）" : "") + " [团队:" + teamId + "]");
        operationLogService.log(log);
    }

    /** 回收站内部名：{name}#del#{id}，保证不超列长且唯一。 */
    private String tombstoneName(String name, Long id) {
        String suffix = "#del#" + id;
        String stem = name.length() > 255 - suffix.length() ? name.substring(0, 255 - suffix.length()) : name;
        return stem + suffix;
    }

    /* ==================== 下载 / 预览 ==================== */

    /**
     * 生成团队文件预签名下载 URL：须为成员；目录/空文件与禁用文件拒绝下载。
     */
    @Override
    public String getDownloadUrl(Long teamId, Long userId, Long fileId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "目录或空文件不可下载");
        }
        // 禁用/对象级禁用文件对成员不可下载（管理端不受此限）
        if (file.getStatus() == FileStatus.DISABLED) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        if (file.getFileHash() != null && !file.getFileHash().isEmpty()
                && disabledObjectMapper.countBlocked(file.getFileHash(), userId) > 0) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        try {
            return storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    /** 团队文件预览（须为成员；内容级校验在预览服务内完成）。 */
    @Override
    public FilePreviewResponse preview(Long teamId, Long userId, Long fileId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        return previewService.previewFile(userId, file);
    }

    /* ==================== 团队回收站 ==================== */

    /** 查询团队回收站记录（须为成员）。 */
    @Override
    public List<RecycleBinResponse> recycleBin(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return recycleBinMapper.findByTeamId(teamId).stream()
                .map(RecycleBinResponse::from)
                .toList();
    }

    /** 从团队回收站恢复（事务）：须为成员且有记录权限，递归恢复子树并校验配额。 */
    @Override
    @Transactional
    public void restore(Long teamId, Long userId, Long recycleId) {
        teamService.requireMember(teamId, userId);
        RecycleBin record = recycleBinMapper.findByIdAndTeamId(recycleId, teamId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        requireRecordPermission(teamId, userId, record);
        restoreRecord(teamId, userId, record);
    }

    /**
     * 递归恢复单条记录：先恢复父（占配额）再恢复子。
     * 删除时子树节点 status 一并置 DELETED 且每节点都有回收站记录，
     * 只恢复顶层会让目录内容丢失，故须递归（子记录 parentId 仍指向原父目录 id）。
     */
    private void restoreRecord(Long teamId, Long userId, RecycleBin record) {
        File file = fileMapper.findById(record.getFileId());
        if (file == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND, "原始文件记录不存在");
        }
        if (file.getParentId() != null && file.getParentId() != FileConstants.ROOT_PARENT_ID) {
            File parent = fileMapper.findById(file.getParentId());
            if (parent == null || parent.getStatus() == FileStatus.DELETED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父目录不可用，请先恢复父目录");
            }
        }
        if (record.getSize() > 0) {
            teamService.checkQuota(teamId, record.getSize());
            teamService.changeUsedSpace(teamId, record.getSize());
        }
        // 还原唯一名（同团队同目录若已被占用则自动追加后缀）
        String uniqueName = resolveTeamUniqueName(teamId, file.getParentId(), record.getOriginalName());
        if (!uniqueName.equals(file.getName())) {
            fileMapper.updateName(record.getFileId(), uniqueName);
        }
        fileMapper.updateStatus(record.getFileId(), FileStatus.NORMAL.getValue());
        recycleBinMapper.deleteById(record.getId());

        if (record.getType() != null && record.getType() == 1) {
            List<RecycleBin> children = recycleBinMapper.findByTeamIdAndParentId(teamId, record.getFileId());
            for (RecycleBin child : children) {
                restoreRecord(teamId, userId, child);
            }
        }

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.RESTORE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(record.getFileId());
        log.setDetail("恢复团队文件: " + record.getOriginalName() + " [团队:" + teamId + "]");
        operationLogService.log(log);
    }

    /** 彻底删除团队回收站记录（事务）：须为成员且有记录权限，复用个人回收站物理清理逻辑。 */
    @Override
    @Transactional
    public void purge(Long teamId, Long userId, Long recycleId) {
        teamService.requireMember(teamId, userId);
        RecycleBin record = recycleBinMapper.findByIdAndTeamId(recycleId, teamId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        requireRecordPermission(teamId, userId, record);
        // 复用个人回收站的物理清理逻辑（递归 + 引用计数释放 + MinIO 删除）
        recycleBinService.purgeRecord(record);
    }

    /* ==================== 管理端 ==================== */

    /** 管理端分页查询团队文件：只校验团队存在，不校验成员身份（权限由管理端拦截器保障）。 */
    @Override
    public Page<FileNodeResponse> adminListFiles(Long teamId, Long parentId, int page, int size) {
        teamService.findById(teamId); // 团队必须存在（管理端不校验成员身份）
        long safeParent = parentId == null ? FileConstants.ROOT_PARENT_ID : parentId;
        int offset = (page - 1) * size;
        long total = fileMapper.countByTeamIdAndParentId(teamId, safeParent);
        List<FileNodeResponse> records = fileMapper.pageByTeamIdAndParentId(teamId, safeParent, offset, size)
                .stream().map(FileNodeResponse::from).toList();
        fillUploaders(records);
        return new Page<>(records, total, page, size);
    }

    /** 管理端查询团队回收站（只校验团队存在）。 */
    @Override
    public List<RecycleBinResponse> adminRecycleBin(Long teamId) {
        teamService.findById(teamId);
        return recycleBinMapper.findByTeamId(teamId).stream()
                .map(RecycleBinResponse::from)
                .toList();
    }

    /** 管理端彻底删除团队回收站记录（只校验团队存在）。 */
    @Override
    @Transactional
    public void adminPurge(Long teamId, Long recycleId) {
        teamService.findById(teamId);
        RecycleBin record = recycleBinMapper.findByIdAndTeamId(recycleId, teamId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        recycleBinService.purgeRecord(record);
    }

    /* ==================== helpers ==================== */

    /** 批量填充上传者显示名（nickname 优先，缺省用 username），避免 N+1 */
    private void fillUploaders(List<FileNodeResponse> records) {
        List<Long> userIds = records.stream()
                .map(FileNodeResponse::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameByUser = userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(
                        com.cloud.backend.entity.User::getId,
                        u -> (u.getNickname() != null && !u.getNickname().isBlank())
                                ? u.getNickname() : u.getUsername(),
                        (a, b) -> a));
        for (FileNodeResponse record : records) {
            if (record.getUserId() != null) {
                record.setUploaderName(nameByUser.get(record.getUserId()));
            }
        }
    }

    /** 团队文件列表必须属于该团队且状态可用（已删除拒绝；禁用仅拒绝下载，见 getTeamFile 调用方） */
    private File getTeamFile(Long teamId, Long fileId) {
        File file = fileMapper.findById(fileId);
        if (file == null || file.getTeamId() == null || file.getTeamId() != teamId) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        // 仅已删除文件拒绝；禁用文件成员仍可见/可管理，仅下载/预览被拒
        if (file.getStatus() == FileStatus.DELETED) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件已在回收站");
        }
        return file;
    }

    private File getTeamDirectory(Long teamId, Long directoryId) {
        File dir = getTeamFile(teamId, directoryId);
        if (!dir.isDir()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标不是目录");
        }
        return dir;
    }

    /**
     * 写权限校验：ADMIN/OWNER 可操作团队所有文件；MEMBER 只能操作自己上传的文件。
     */
    private void requireFileWritePermission(Long teamId, Long userId, File file) {
        TeamMemberRole role = teamService.getMyRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }
        if (role.getValue() >= TeamMemberRole.ADMIN.getValue()) {
            return;
        }
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED, "只能操作自己上传的文件");
        }
    }

    /** 回收站记录权限：ADMIN/OWNER 任意；MEMBER 只能操作自己上传的记录 */
    private void requireRecordPermission(Long teamId, Long userId, RecycleBin record) {
        TeamMemberRole role = teamService.getMyRole(teamId, userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_MEMBER);
        }
        if (role.getValue() >= TeamMemberRole.ADMIN.getValue()) {
            return;
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TEAM_PERMISSION_DENIED, "只能操作自己上传的文件");
        }
    }

    /** 团队空间同名唯一化（同团队同目录 name 唯一，跨 user_id 共享命名空间） */
    private String resolveTeamUniqueName(Long teamId, Long parentId, String baseName) {
        String name = baseName;
        int suffix = 2;
        while (fileMapper.findByTeamIdAndParentIdAndName(teamId, parentId, name) != null) {
            String stem = baseName.lastIndexOf('.') > 0
                    ? baseName.substring(0, baseName.lastIndexOf('.'))
                    : baseName;
            String ext = baseName.lastIndexOf('.') > 0
                    ? baseName.substring(baseName.lastIndexOf('.'))
                    : "";
            name = stem + "（" + suffix + "）" + ext;
            suffix++;
            if (suffix > 1000) {
                throw new BusinessException(ErrorCode.FILE_NAME_DUPLICATE);
            }
        }
        return name;
    }

    /** 构建 path（父目录 path + "/" + name），根目录下为 "/name" */
    private String buildPath(Long parentId, String name) {
        if (parentId == null || parentId == FileConstants.ROOT_PARENT_ID) {
            return "/" + name;
        }
        File parent = fileMapper.findById(parentId);
        if (parent == null || parent.getPath() == null) {
            return "/" + name;
        }
        return parent.getPath() + "/" + name;
    }

    /** BFS 收集子树（含根节点，团队维度；仅收集正常状态节点，避免重复回收已删除子树） */
    private List<File> collectSubtree(Long teamId, Long rootId) {
        File root = fileMapper.findById(rootId);
        if (root == null || root.getStatus() != FileStatus.NORMAL) {
            return List.of();
        }
        Map<Long, List<File>> childrenByParent = fileMapper.findByTeamId(teamId).stream()
                .filter(f -> f.getStatus() == FileStatus.NORMAL)
                .collect(Collectors.groupingBy(File::getParentId));
        List<File> result = new ArrayList<>();
        result.add(root);
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<File> children = childrenByParent.getOrDefault(current, List.of());
            for (File child : children) {
                result.add(child);
                if (child.isDir()) {
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }
}
