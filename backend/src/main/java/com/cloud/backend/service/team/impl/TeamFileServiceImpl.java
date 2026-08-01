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
 * 团队文件服务实现（docs/team-module.md §四/§六）。
 * 参照 FileServiceImpl 的目录树模型，但所有查询带 teamId 维度；
 * 团队成员共享同一命名空间（同团队同目录 name 唯一，跨 user_id）。
 * 删除进团队回收站（带 team_id），恢复/彻底删除权限同文件管理权限。
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

    public TeamFileServiceImpl(FileMapper fileMapper, FileHashMapper fileHashMapper,
                               RecycleBinMapper recycleBinMapper, StorageService storageService,
                               TeamService teamService, PreviewService previewService,
                               OperationLogService operationLogService, AdminSettingsService adminSettingsService,
                               com.cloud.backend.service.file.RecycleBinService recycleBinService) {
        this.fileMapper = fileMapper;
        this.fileHashMapper = fileHashMapper;
        this.recycleBinMapper = recycleBinMapper;
        this.storageService = storageService;
        this.teamService = teamService;
        this.previewService = previewService;
        this.operationLogService = operationLogService;
        this.adminSettingsService = adminSettingsService;
        this.recycleBinService = recycleBinService;
    }

    /* ==================== 列表 / 树 / 目录 ==================== */

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
        return new Page<>(records, total, page, size);
    }

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

    private FileTreeResponse buildTree(File dir, Map<Long, List<File>> childrenByParent) {
        FileTreeResponse node = FileTreeResponse.of(dir.getId(), dir.getName(), true);
        for (File child : childrenByParent.getOrDefault(dir.getId(), List.of())) {
            node.getChildren().add(buildTree(child, childrenByParent));
        }
        return node;
    }

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

    private String tombstoneName(String name, Long id) {
        String suffix = "#del#" + id;
        String stem = name.length() > 255 - suffix.length() ? name.substring(0, 255 - suffix.length()) : name;
        return stem + suffix;
    }

    /* ==================== 下载 / 预览 ==================== */

    @Override
    public String getDownloadUrl(Long teamId, Long userId, Long fileId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "目录或空文件不可下载");
        }
        try {
            return storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    @Override
    public FilePreviewResponse preview(Long teamId, Long userId, Long fileId) {
        teamService.requireMember(teamId, userId);
        File file = getTeamFile(teamId, fileId);
        return previewService.previewFile(userId, file);
    }

    /* ==================== 团队回收站 ==================== */

    @Override
    public List<RecycleBinResponse> recycleBin(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return recycleBinMapper.findByTeamId(teamId).stream()
                .map(RecycleBinResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void restore(Long teamId, Long userId, Long recycleId) {
        teamService.requireMember(teamId, userId);
        RecycleBin record = recycleBinMapper.findByIdAndTeamId(recycleId, teamId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        requireRecordPermission(teamId, userId, record);

        File file = fileMapper.findById(record.getFileId());
        if (file == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND, "原始文件记录不存在");
        }
        if (file.getParentId() != null && file.getParentId() != FileConstants.ROOT_PARENT_ID) {
            File parent = fileMapper.findById(file.getParentId());
            if (parent == null || parent.getStatus() != FileStatus.NORMAL) {
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

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.RESTORE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(record.getFileId());
        log.setDetail("恢复团队文件: " + record.getOriginalName() + " [团队:" + teamId + "]");
        operationLogService.log(log);
    }

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

    @Override
    public Page<FileNodeResponse> adminListFiles(Long teamId, Long parentId, int page, int size) {
        teamService.findById(teamId); // 团队必须存在（管理端不校验成员身份）
        long safeParent = parentId == null ? FileConstants.ROOT_PARENT_ID : parentId;
        int offset = (page - 1) * size;
        long total = fileMapper.countByTeamIdAndParentId(teamId, safeParent);
        List<FileNodeResponse> records = fileMapper.pageByTeamIdAndParentId(teamId, safeParent, offset, size)
                .stream().map(FileNodeResponse::from).toList();
        return new Page<>(records, total, page, size);
    }

    @Override
    public List<RecycleBinResponse> adminRecycleBin(Long teamId) {
        teamService.findById(teamId);
        return recycleBinMapper.findByTeamId(teamId).stream()
                .map(RecycleBinResponse::from)
                .toList();
    }

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

    /** 团队文件必须属于该团队且状态正常 */
    private File getTeamFile(Long teamId, Long fileId) {
        File file = fileMapper.findById(fileId);
        if (file == null || file.getTeamId() == null || file.getTeamId() != teamId) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (file.getStatus() != FileStatus.NORMAL) {
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
     * 写权限校验（docs/team-module.md §三 权限矩阵）：
     * ADMIN/OWNER 可操作团队所有文件；MEMBER 只能操作自己上传的文件。
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
