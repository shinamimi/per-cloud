package com.cloud.backend.service.admin.impl;

import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dto.AdminFileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.admin.AdminFileResponse;
import com.cloud.backend.dto.admin.AdminRecycleResponse;
import com.cloud.backend.entity.DisabledObject;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.entity.Team;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.DisabledObjectMapper;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.RecycleBinMapper;
import com.cloud.backend.mapper.TeamMapper;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.service.admin.AdminFileService;
import com.cloud.backend.service.admin.AdminSettingsService;
import com.cloud.backend.service.file.RecycleBinService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.team.TeamService;
import com.cloud.backend.service.user.UserService;
import com.cloud.backend.utils.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端全局文件管控实现 —— 全局文件列表、禁用/启用、全局回收站与恢复/彻底删除。
 *
 * 设计思路：
 * - 全局列表个人+团队统一（team_id=0 个人，>0 团队），userId/teamId/category/status 动态筛选
 * - 禁用/启用：改 t_file.status=2/1；用户侧列表 status != 0 仍可见，下载/预览拒绝
 * - 删除进全局回收站：tombstone 顶层名 + 递归子树 + t_recycle_bin.deleted_by=1 + 释放对应空间配额
 * - 恢复：递归恢复子树（删除时子节点 status 一并置 DELETED，仅恢复顶层会丢内容）；
 *   个人空间校验用户配额，团队空间校验团队配额
 * - 彻底删除：复用个人回收站物理清理（递归 + 秒传引用归零 + MinIO 删除）
 */
@Service
public class AdminFileServiceImpl implements AdminFileService {

    private final FileMapper fileMapper;
    private final RecycleBinMapper recycleBinMapper;
    private final RecycleBinService recycleBinService;
    private final DisabledObjectMapper disabledObjectMapper;
    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final UserService userService;
    private final TeamService teamService;
    private final OperationLogService operationLogService;
    private final AdminSettingsService adminSettingsService;
    private final StorageService storageService;

    public AdminFileServiceImpl(FileMapper fileMapper, RecycleBinMapper recycleBinMapper,
                                RecycleBinService recycleBinService, DisabledObjectMapper disabledObjectMapper,
                                UserMapper userMapper, TeamMapper teamMapper, UserService userService,
                                TeamService teamService, OperationLogService operationLogService,
                                AdminSettingsService adminSettingsService, StorageService storageService) {
        this.fileMapper = fileMapper;
        this.recycleBinMapper = recycleBinMapper;
        this.recycleBinService = recycleBinService;
        this.disabledObjectMapper = disabledObjectMapper;
        this.userMapper = userMapper;
        this.teamMapper = teamMapper;
        this.userService = userService;
        this.teamService = teamService;
        this.operationLogService = operationLogService;
        this.adminSettingsService = adminSettingsService;
        this.storageService = storageService;
    }

    /**
     * 分页查询全局文件列表（个人与团队统一），并补充归属者显示名与禁用来源。
     * 权限约束：仅 ADMIN+ 可调；页号非法时按 1 兜底。
     */
    @Override
    public Page<AdminFileResponse> page(AdminFileQuery query) {
        int safePage = Math.max(query.getPage(), 1);
        query.setOffset((safePage - 1) * query.getSize());
        long total = fileMapper.adminCount(query);
        List<AdminFileResponse> records = fileMapper.adminPage(query).stream()
                .map(AdminFileResponse::from)
                .toList();
        fillOwners(records);
        fillDisabledScope(records);
        return new Page<>(records, total, safePage, query.getSize());
    }

    /** 查询单个文件详情（含归属者与禁用来源）；文件不存在抛业务异常。 */
    @Override
    public AdminFileResponse detail(Long id) {
        File file = fileMapper.findById(id);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        AdminFileResponse response = AdminFileResponse.from(file);
        fillOwners(List.of(response));
        fillDisabledScope(List.of(response));
        return response;
    }

    /** 填充禁用来源（status=DISABLED 时查对象级禁用记录：GLOBAL=全站禁，否则该用户 USER 禁） */
    private void fillDisabledScope(List<AdminFileResponse> records) {
        for (AdminFileResponse record : records) {
            if (record.getStatus() != FileStatus.DISABLED || record.getFileHash() == null || record.getFileHash().isEmpty()) {
                continue;
            }
            List<DisabledObject> disables = disabledObjectMapper.findByHash(record.getFileHash());
            boolean global = disables.stream()
                    .anyMatch(d -> d.getScope() == DisableScope.GLOBAL.getValue());
            if (global) {
                record.setDisabledScope(DisableScope.GLOBAL.name());
            } else if (disables.stream().anyMatch(d -> d.getScope() == DisableScope.USER.getValue()
                    && d.getUserId() != null && d.getUserId().equals(record.getUserId()))) {
                record.setDisabledScope(DisableScope.USER.name());
            }
        }
    }

    /** 取可下载的文件实体：文件必须存在且为含对象名的普通文件，目录/空文件拒绝。 */
    @Override
    public File detailEntity(Long id) {
        File file = fileMapper.findById(id);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "目录或空文件不可下载");
        }
        return file;
    }

    /** 生成带有效期（管理员配置分钟数）的预签名下载地址；失败转业务异常。 */
    @Override
    public String generateDownloadUrl(File file) {
        try {
            return storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED, e.getMessage());
        }
    }

    /**
     * 禁用/启用文件（事务 + 写操作日志）：有内容 hash 的走对象级禁用记录，
     * 无 hash 的直接改文件状态；启用时先解除禁用记录再重放其余记录。
     * 权限约束：仅 ADMIN+ 可调；状态仅允许 NORMAL/DISABLED。
     */
    @Override
    @Transactional
    public void changeStatus(Long id, FileStatus status, DisableScope scope) {
        if (status == null || status == FileStatus.DELETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 NORMAL/DISABLED");
        }
        File file = fileMapper.findById(id);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        boolean hasHash = file.getFileHash() != null && !file.getFileHash().isEmpty();
        if (status == FileStatus.DISABLED) {
            if (hasHash) {
                disableObject(file, scope == null ? DisableScope.USER : scope);
            } else {
                fileMapper.updateStatus(id, FileStatus.DISABLED.getValue());
            }
        } else {
            if (hasHash) {
                // 启用不依赖前端 scope：同时解除全站禁与"该用户仅用户禁"，再重放剩余禁用记录
                enableObject(file);
            } else {
                fileMapper.updateStatus(id, FileStatus.NORMAL.getValue());
            }
        }

        OperationLog log = new OperationLog();
        log.setUserId(AuthorizationPolicy.getCurrentUserId());
        log.setOperation(status == FileStatus.DISABLED ? OperationType.DISABLE_FILE : OperationType.ENABLE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(id);
        log.setDetail((status == FileStatus.DISABLED ? "禁用文件: " : "启用文件: ") + file.getName()
                + (status == FileStatus.DISABLED && scope == DisableScope.GLOBAL ? "（全站禁）" : ""));
        operationLogService.log(log);
    }

    /** 对象级禁用：写禁用记录（重复记录幂等忽略）+ 按范围更新文件状态（全站禁按 hash 全量，仅用户禁按 hash+用户） */
    private void disableObject(File file, DisableScope scope) {
        DisabledObject record = new DisabledObject();
        record.setFileHash(file.getFileHash());
        record.setScope(scope.getValue());
        record.setUserId(scope == DisableScope.USER ? file.getUserId() : 0L);
        record.setCreatedBy(AuthorizationPolicy.getCurrentUserId());
        try {
            disabledObjectMapper.insert(record);
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // 已存在相同禁用记录，幂等
        }
        if (scope == DisableScope.GLOBAL) {
            fileMapper.disableByHash(file.getFileHash());
        } else {
            fileMapper.disableByHashAndUser(file.getFileHash(), file.getUserId());
        }
    }

    /** 对象级启用：解除全站禁 + 该用户仅用户禁，删除记录后重算（先全部恢复，再重放剩余禁用记录） */
    private void enableObject(File file) {
        String hash = file.getFileHash();
        disabledObjectMapper.deleteByHashAndScopeAndUser(hash, DisableScope.GLOBAL.getValue(), 0L);
        disabledObjectMapper.deleteByHashAndScopeAndUser(hash, DisableScope.USER.getValue(), file.getUserId());
        fileMapper.restoreByHash(hash);
        for (DisabledObject record : disabledObjectMapper.findByHash(hash)) {
            if (record.getScope() == DisableScope.GLOBAL.getValue()) {
                fileMapper.disableByHash(record.getFileHash());
            } else {
                fileMapper.disableByHashAndUser(record.getFileHash(), record.getUserId());
            }
        }
    }

    /**
     * 批量删除到全局回收站（事务）：子树整体置 DELETED、逐节点写回收站记录，
     * 顶层节点改内部名避免占用唯一索引，并释放对应空间配额、写操作日志。
     * 回收站记录标记为系统管理员删除，保留天数按个人/团队配置区分。
     */
    @Override
    @Transactional
    public void deleteToGlobalRecycleBin(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            File file = fileMapper.findById(id);
            if (file == null) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            if (file.getStatus() == FileStatus.DELETED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "文件已在回收站");
            }
            boolean teamSpace = file.getTeamId() != null && file.getTeamId() > 0;
            List<File> nodes = collectSubtree(file);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.plusDays(teamSpace
                    ? adminSettingsService.getTeamRecycleBinDays()
                    : adminSettingsService.getRecycleBinDays());
            String originalName = file.getName();
            fileMapper.updateName(id, tombstoneName(originalName, id));
            long totalSize = 0;
            for (File node : nodes) {
                totalSize += node.getSize() != null ? node.getSize() : 0;
                fileMapper.updateStatus(node.getId(), FileStatus.DELETED.getValue());
                RecycleBin record = new RecycleBin();
                record.setUserId(node.getUserId());
                record.setFileId(node.getId());
                record.setOriginalName(node.getId().equals(id) ? originalName : node.getName());
                record.setObjectName(node.getObjectName() == null ? "" : node.getObjectName());
                record.setFileHash(node.getFileHash() == null ? "" : node.getFileHash());
                record.setType(node.isDir() ? 1 : 0);
                record.setTeamId(node.getTeamId() == null ? 0L : node.getTeamId());
                record.setDeletedBy(1);
                record.setParentId(node.getParentId());
                record.setSize(node.getSize() == null ? 0 : node.getSize());
                record.setMimeType(node.getMimeType() == null ? "" : node.getMimeType());
                record.setDeletedTime(now);
                record.setExpireTime(expireTime);
                recycleBinMapper.insert(record);
            }
            if (totalSize > 0) {
                if (teamSpace) {
                    teamService.changeUsedSpace(file.getTeamId(), -totalSize);
                } else {
                    userService.changeUsedSpace(file.getUserId(), -totalSize);
                }
            }

            OperationLog log = new OperationLog();
            log.setUserId(AuthorizationPolicy.getCurrentUserId());
            log.setOperation(OperationType.DELETE_FILE);
            log.setTargetType(TargetType.FILE);
            log.setTargetId(id);
            log.setDetail("管理员删除文件: " + file.getName()
                    + (nodes.size() > 1 ? "（含 " + (nodes.size() - 1) + " 个子项）" : "")
                    + (teamSpace ? " [团队:" + file.getTeamId() + "]" : " [用户:" + file.getUserId() + "]"));
            operationLogService.log(log);
        }
    }

    /** 查询全局回收站记录（含归属者显示名）。 */
    @Override
    public List<AdminRecycleResponse> globalRecycleBin() {
        List<AdminRecycleResponse> records = recycleBinMapper.findGlobal().stream()
                .map(AdminRecycleResponse::from)
                .toList();
        fillRecycleOwners(records);
        return records;
    }

    /** 从全局回收站恢复单条记录（事务）：递归恢复子树并校验配额，见 restoreRecord。 */
    @Override
    @Transactional
    public void restore(Long recycleId) {
        RecycleBin record = recycleBinMapper.findGlobalById(recycleId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        restoreRecord(record);
    }

    /** 批量彻底删除回收站记录（事务）：复用个人回收站物理清理（递归 + 引用计数归零 + 存储删除）。 */
    @Override
    @Transactional
    public void purge(List<Long> recycleIds) {
        if (recycleIds == null || recycleIds.isEmpty()) {
            return;
        }
        for (Long recycleId : recycleIds) {
            RecycleBin record = recycleBinMapper.findGlobalById(recycleId);
            if (record == null) {
                throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
            }
            recycleBinService.purgeRecord(record);
        }
    }

    /* ==================== helpers ==================== */

    /** 递归收集子树（个人/团队统一；禁用子节点一并处理） */
    private List<File> collectSubtree(File top) {
        List<File> nodes = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(top);
        while (!stack.isEmpty()) {
            File node = stack.pop();
            nodes.add(node);
            if (node.isDir()) {
                List<File> children = (node.getTeamId() != null && node.getTeamId() > 0)
                        ? fileMapper.findByTeamIdAndParentId(node.getTeamId(), node.getId())
                        : fileMapper.findByUserIdAndParentId(node.getUserId(), node.getId());
                children.forEach(stack::push);
            }
        }
        return nodes;
    }

    /**
     * 递归恢复：顶层 + 子记录（删除时子节点 status 一并置 DELETED，只恢复顶层会让目录内容丢失）。
     * 先恢复父（占配额）再恢复子；个人空间校验用户配额，团队空间校验团队配额。
     */
    private void restoreRecord(RecycleBin record) {
        File file = fileMapper.findById(record.getFileId());
        if (file == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND, "原始文件记录不存在");
        }
        boolean teamSpace = record.getTeamId() != null && record.getTeamId() > 0;
        if (file.getParentId() != null && file.getParentId() != FileConstants.ROOT_PARENT_ID) {
            File parent = fileMapper.findById(file.getParentId());
            if (parent == null || parent.getStatus() == FileStatus.DELETED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父目录不可用，请先恢复父目录");
            }
        }
        if (record.getSize() > 0) {
            if (teamSpace) {
                teamService.checkQuota(record.getTeamId(), record.getSize());
                teamService.changeUsedSpace(record.getTeamId(), record.getSize());
            } else {
                long remaining = userService.getRemainingQuota(record.getUserId());
                if (record.getSize() > remaining) {
                    throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED, "恢复后空间不足，请先清理其他文件");
                }
                userService.changeUsedSpace(record.getUserId(), record.getSize());
            }
        }
        String uniqueName = teamSpace
                ? resolveTeamUniqueName(record.getTeamId(), file.getParentId(), record.getOriginalName())
                : FileUtil.resolveUniqueName(fileMapper, record.getUserId(), file.getParentId(), record.getOriginalName());
        if (!uniqueName.equals(file.getName())) {
            fileMapper.updateName(record.getFileId(), uniqueName);
        }
        fileMapper.updateStatus(record.getFileId(), FileStatus.NORMAL.getValue());
        recycleBinMapper.deleteById(record.getId());

        if (record.getType() != null && record.getType() == 1) {
            List<RecycleBin> children = teamSpace
                    ? recycleBinMapper.findGlobalChildrenByTeamId(record.getTeamId(), record.getFileId())
                    : recycleBinMapper.findGlobalChildrenByUserId(record.getUserId(), record.getFileId());
            for (RecycleBin child : children) {
                restoreRecord(child);
            }
        }

        OperationLog log = new OperationLog();
        log.setUserId(AuthorizationPolicy.getCurrentUserId());
        log.setOperation(OperationType.RESTORE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(record.getFileId());
        log.setDetail("管理员恢复文件: " + record.getOriginalName()
                + (teamSpace ? " [团队:" + record.getTeamId() + "]" : " [用户:" + record.getUserId() + "]"));
        operationLogService.log(log);
    }

    /** 顶层节点改内部名，避免占用唯一索引（同空间同目录 name 唯一） */
    private String tombstoneName(String name, Long id) {
        String suffix = "#del#" + id;
        String stem = name.length() > 255 - suffix.length() ? name.substring(0, 255 - suffix.length()) : name;
        return stem + suffix;
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
        }
        return name;
    }

    /** 批量填充所属用户/团队显示名（避免 N+1：用户批量查，团队循环查） */
    private void fillOwners(List<AdminFileResponse> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(AdminFileResponse::getUserId).filter(Objects::nonNull).distinct().toList();
        if (!userIds.isEmpty()) {
            Map<Long, String> nameByUser = userMapper.findByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> displayName(u), (a, b) -> a));
            for (AdminFileResponse record : records) {
                if (record.getUserId() != null) {
                    record.setUserName(nameByUser.get(record.getUserId()));
                }
            }
        }
        Set<Long> teamIds = records.stream().map(AdminFileResponse::getTeamId)
                .filter(id -> id != null && id > 0).collect(Collectors.toSet());
        for (Long teamId : teamIds) {
            Team team = teamMapper.findById(teamId);
            String teamName = team == null ? "团队#" + teamId : team.getName();
            for (AdminFileResponse record : records) {
                if (teamId.equals(record.getTeamId())) {
                    record.setTeamName(teamName);
                }
            }
        }
    }

    /** 批量填充回收站记录所属用户/团队显示名（避免 N+1：用户批量查，团队循环查） */
    private void fillRecycleOwners(List<AdminRecycleResponse> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(AdminRecycleResponse::getUserId).filter(Objects::nonNull).distinct().toList();
        if (!userIds.isEmpty()) {
            Map<Long, String> nameByUser = userMapper.findByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> displayName(u), (a, b) -> a));
            for (AdminRecycleResponse record : records) {
                if (record.getUserId() != null) {
                    record.setUserName(nameByUser.get(record.getUserId()));
                }
            }
        }
        Set<Long> teamIds = records.stream().map(AdminRecycleResponse::getTeamId)
                .filter(id -> id != null && id > 0).collect(Collectors.toSet());
        for (Long teamId : teamIds) {
            Team team = teamMapper.findById(teamId);
            String teamName = team == null ? "团队#" + teamId : team.getName();
            for (AdminRecycleResponse record : records) {
                if (teamId.equals(record.getTeamId())) {
                    record.setTeamName(teamName);
                }
            }
        }
    }

    /** 用户显示名：nickname 非空取 nickname，否则回退 username */
    private String displayName(User user) {
        return (user.getNickname() != null && !user.getNickname().isBlank())
                ? user.getNickname() : user.getUsername();
    }
}
