package com.cloud.backend.service.file.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.dao.FileDao;
import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import com.cloud.backend.utils.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final StorageService storageService;
    private final OperationLogService operationLogService;
    private final UserService userService;
    private final FileHashMapper fileHashMapper;
    private final com.cloud.backend.config.FileProperties fileProperties;
    private final com.cloud.backend.service.file.RecycleBinService recycleBinService;
    private final com.cloud.backend.mapper.RecycleBinMapper recycleBinMapper;
    private final com.cloud.backend.dao.FileDao fileDao;
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;

    public FileServiceImpl(FileMapper fileMapper, StorageService storageService,
                           OperationLogService operationLogService, UserService userService,
                           FileHashMapper fileHashMapper, com.cloud.backend.config.FileProperties fileProperties,
                           com.cloud.backend.service.file.RecycleBinService recycleBinService,
                           com.cloud.backend.mapper.RecycleBinMapper recycleBinMapper,
                           com.cloud.backend.dao.FileDao fileDao,
                           com.cloud.backend.service.admin.AdminSettingsService adminSettingsService) {
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.operationLogService = operationLogService;
        this.userService = userService;
        this.fileHashMapper = fileHashMapper;
        this.fileProperties = fileProperties;
        this.recycleBinService = recycleBinService;
        this.recycleBinMapper = recycleBinMapper;
        this.fileDao = fileDao;
        this.adminSettingsService = adminSettingsService;
    }

    /** 新增文件/目录记录（调用方负责填充全部业务字段）。 */
    @Override
    public File save(File file) {
        fileMapper.insert(file);
        return file;
    }

    /** 按 id 查询文件记录（不做归属校验，调用方需自行确认）。 */
    @Override
    public File findById(Long id) {
        return fileMapper.findById(id);
    }

    /** 查询某用户某目录下的直接子项。 */
    @Override
    public List<File> listByUserAndParent(Long userId, Long parentId) {
        return fileMapper.findByUserIdAndParentId(userId, parentId);
    }

    /** 按用户 + 全路径查询文件记录。 */
    @Override
    public File findByPath(Long userId, String path) {
        return fileMapper.findByUserIdAndPath(userId, path);
    }

    /** 更新文件记录，返回受影响行数。 */
    @Override
    public int update(File file) {
        return fileMapper.update(file);
    }

    /** 物理删除文件记录（注意：业务删除应走"删除到回收站"流程）。 */
    @Override
    public int removeById(Long id) {
        return fileMapper.deleteById(id);
    }

    /** 更新文件状态（取值见 FileStatus 枚举），返回受影响行数。 */
    @Override
    public int updateStatus(Long id, Integer status) {
        return fileMapper.updateStatus(id, status);
    }

    /** 查询全部文件记录（管理端/定时任务用）。 */
    @Override
    public List<File> findAll() {
        return fileMapper.findAll();
    }

    /** 分页查询用户某目录下的直接子项；非根目录时先校验目录归属。 */
    @Override
    public Page<FileNodeResponse> pageByUserAndParent(Long userId, Long parentId, int page, int size) {
        if (parentId != null && parentId != FileConstants.ROOT_PARENT_ID) {
            getOwnedDirectory(userId, parentId);
        }
        int offset = (page - 1) * size;
        long total = fileMapper.countByUserIdAndParentId(userId, parentId);
        List<FileNodeResponse> records = fileMapper.pageByUserIdAndParentId(userId, parentId, offset, size)
                .stream().map(FileNodeResponse::from).toList();
        return new Page<>(records, total, page, size);
    }

    /** 分页查询用户音频类文件（按分类筛选，跨目录）。 */
    @Override
    public Page<FileNodeResponse> listAudio(Long userId, int page, int size) {
        FileQuery query = new FileQuery();
        query.setUserId(userId);
        query.setCategory(FileConstants.AUDIO);
        query.setOffset((page - 1) * size);
        query.setSize(size);
        long total = fileDao.count(query);
        List<FileNodeResponse> records = fileDao.searchPage(query)
                .stream().map(FileNodeResponse::from).toList();
        return new Page<>(records, total, page, size);
    }

    /** 构建用户目录树（仅目录节点，根目录下为顶层目录）。 */
    @Override
    public List<FileTreeResponse> tree(Long userId) {
        Map<Long, List<File>> childrenByParent = fileMapper.findByUserId(userId).stream()
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

    /**
     * 创建目录（写操作日志）：校验名称长度与父目录归属，同名自动加"（2）"后缀。
     * 仅本人空间生效，父目录必须属于该用户。
     */
    @Override
    @Log(operation = OperationType.CREATE_DIRECTORY, target = TargetType.FILE, targetId = "#request.parentId",
         detail = "'创建目录: ' + #request.name")
    public File createDirectory(Long userId, DirectoryCreateRequest request) {
        String name = request.getName().trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目录名长度需在 1-255 之间");
        }
        if (request.getParentId() != FileConstants.ROOT_PARENT_ID) {
            getOwnedDirectory(userId, request.getParentId());
        }
        String uniqueName = resolveUniqueName(userId, request.getParentId(), name);
        File dir = new File();
        dir.setUserId(userId);
        dir.setTeamId(0L);
        dir.setParentId(request.getParentId());
        dir.setName(uniqueName);
        dir.setPath(buildPath(userId, request.getParentId(), uniqueName));
        dir.setSize(0L);
        dir.setIsDirectory(1);
        dir.setType(FileType.DIRECTORY);
        dir.setCategory(FileConstants.OTHER);
        dir.setObjectName("");
        dir.setStatus(FileStatus.NORMAL);
        fileMapper.insert(dir);
        return dir;
    }

    /** 重命名文件/目录：校验名称合法性与归属，目标名冲突时自动加序号后缀。 */
    @Override
    public FileNodeResponse rename(Long userId, Long fileId, String name) {
        String newName = name.trim();
        if (newName.isEmpty() || newName.length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名长度需在 1-255 之间");
        }
        File file = getOwnedFile(userId, fileId);
        String uniqueName = resolveUniqueName(userId, file.getParentId(), newName);
        fileMapper.updateName(fileId, uniqueName);
        file.setName(uniqueName);
        return FileNodeResponse.from(file);
    }

    /** 移动文件/目录：目标目录须属于该用户；不能移动到自身或其子目录下；目标位置同名冲突时自动加序号后缀。 */
    @Override
    public FileNodeResponse move(Long userId, Long fileId, Long targetParentId) {
        File file = getOwnedFile(userId, fileId);
        if (targetParentId != FileConstants.ROOT_PARENT_ID) {
            getOwnedDirectory(userId, targetParentId);
        }
        if (targetParentId.equals(fileId)) {
            throw new BusinessException(ErrorCode.MOVE_INVALID);
        }
        if (file.isDir()) {
            // 不能移动到自己的子目录下
            for (Long childId : collectSubtree(userId, fileId).stream().map(File::getId).toList()) {
                if (targetParentId.equals(childId)) {
                    throw new BusinessException(ErrorCode.MOVE_INVALID);
                }
            }
        }
        String uniqueName = resolveUniqueName(userId, targetParentId, file.getName());
        fileMapper.updateParent(fileId, targetParentId);
        if (!uniqueName.equals(file.getName())) {
            fileMapper.updateName(fileId, uniqueName);
            file.setName(uniqueName);
        }
        file.setParentId(targetParentId);
        return FileNodeResponse.from(file);
    }

    /**
     * 复制文件/目录到目标目录（事务）：目录递归复制，文件复制共享对象存储引用（引用计数 +1）；
     * 复制产生的新文件按总大小扣减个人配额，配额不足整体回滚。
     */
    @Override
    @Transactional
    public FileNodeResponse copy(Long userId, Long fileId, Long targetParentId) {
        File file = getOwnedFile(userId, fileId);
        if (targetParentId != FileConstants.ROOT_PARENT_ID) {
            getOwnedDirectory(userId, targetParentId);
        }
        if (targetParentId.equals(fileId)) {
            throw new BusinessException(ErrorCode.MOVE_INVALID, "不能复制到自身");
        }
        Map<Long, Long> idMap = new HashMap<>();
        File copied = copyNode(userId, file, targetParentId, idMap);
        long extraSize = 0;
        if (file.isDir()) {
            List<File> children = collectSubtree(userId, fileId);
            for (File child : children) {
                if (child.getId().equals(fileId)) {
                    continue;
                }
                Long newParent = child.getParentId().equals(fileId)
                        ? copied.getId()
                        : idMap.get(child.getParentId());
                if (newParent == null) {
                    newParent = targetParentId;
                }
                File copiedChild = copyNode(userId, child, newParent, idMap);
                if (!child.isDir() && child.getSize() != null) {
                    extraSize += child.getSize();
                }
            }
        }
        if (extraSize > 0) {
            long remaining = userService.getRemainingQuota(userId);
            if (extraSize > remaining) {
                throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED, "复制后空间不足");
            }
            userService.changeUsedSpace(userId, extraSize);
        }
        return FileNodeResponse.from(copied);
    }

    /** 复制单个节点：新增 t_file 记录；文件引用共享对象（秒传引用计数 +1） */
    private File copyNode(Long userId, File source, Long targetParentId, Map<Long, Long> idMap) {
        String uniqueName = resolveUniqueName(userId, targetParentId, source.getName());
        File copy = new File();
        copy.setUserId(userId);
        copy.setTeamId(0L);
        copy.setParentId(targetParentId);
        copy.setName(uniqueName);
        copy.setPath(buildPath(userId, targetParentId, uniqueName));
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

    /**
     * 删除到回收站（事务）：子树整体置 DELETED，批量写回收站记录（顶层用删除前原名），
     * 释放配额并写操作日志；顶层节点改内部名避免占用唯一索引。
     */
    @Override
    @Transactional
    public void deleteToRecycle(Long userId, Long fileId) {
        File file = getOwnedFile(userId, fileId);
        // 顶层节点改内部名，避免占用唯一索引（同名文件可再次创建；回收站显示用 original_name）
        String originalName = file.getName();
        String tombstone = tombstoneName(originalName, fileId);
        fileMapper.updateName(fileId, tombstone);
        List<File> nodes = collectSubtree(userId, fileId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(adminSettingsService.getRecycleBinDays());
        long totalSize = 0;
        // 批量更新状态（替代循环单条 UPDATE）
        List<Long> nodeIds = nodes.stream().map(File::getId).toList();
        fileMapper.updateStatusByIds(nodeIds, FileStatus.DELETED.getValue());
        // 批量构建回收站记录
        List<RecycleBin> recycleBins = new ArrayList<>();
        for (File node : nodes) {
            totalSize += node.getSize() != null ? node.getSize() : 0;
            RecycleBin recycleBin = new RecycleBin();
            recycleBin.setUserId(userId);
            recycleBin.setFileId(node.getId());
            recycleBin.setOriginalName(node.getId().equals(fileId) ? originalName : node.getName());
            recycleBin.setObjectName(node.getObjectName() == null ? "" : node.getObjectName());
            recycleBin.setFileHash(node.getFileHash() == null ? "" : node.getFileHash());
            recycleBin.setType(node.isDir() ? 1 : 0);
            recycleBin.setTeamId(0L);
            recycleBin.setDeletedBy(0);
            recycleBin.setParentId(node.getParentId());
            recycleBin.setSize(node.getSize() == null ? 0 : node.getSize());
            recycleBin.setMimeType(node.getMimeType() == null ? "" : node.getMimeType());
            recycleBin.setDeletedTime(now);
            recycleBin.setExpireTime(expireTime);
            recycleBins.add(recycleBin);
        }
        // 批量插入回收站（替代循环单条 INSERT）
        recycleBinMapper.batchInsert(recycleBins);
        if (totalSize > 0) {
            userService.changeUsedSpace(userId, -totalSize);
        }
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(OperationType.DELETE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(fileId);
        log.setDetail("删除文件/目录: " + file.getName() + (nodes.size() > 1 ? "（含 " + (nodes.size() - 1) + " 个子项）" : ""));
        operationLogService.log(log);
    }

    /** 回收站内部名：{name}#del#{id}，保证不超列长且唯一 */
    private String tombstoneName(String name, Long id) {
        String suffix = "#del#" + id;
        String stem = name.length() > 255 - suffix.length() ? name.substring(0, 255 - suffix.length()) : name;
        return stem + suffix;
    }

    /**
     * 取用户自己的可用文件：校验归属（个人空间 teamId=0）；
     * 已删除文件拒绝，禁用文件仍可见（下载/预览在下载服务中另行拒绝）。
     */
    @Override
    public File getOwnedFile(Long userId, Long fileId) {
        File file = fileMapper.findById(fileId);
        if (file == null || !file.getUserId().equals(userId) || file.getTeamId() != null && file.getTeamId() != 0) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        // 仅已删除文件拒绝；禁用文件用户仍可见/可管理，仅下载/预览被拒
        if (file.getStatus() == FileStatus.DELETED) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件已在回收站");
        }
        return file;
    }

    /** 取用户自己的目录（非目录抛业务异常）。 */
    private File getOwnedDirectory(Long userId, Long directoryId) {
        File dir = getOwnedFile(userId, directoryId);
        if (!dir.isDir()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标不是目录");
        }
        return dir;
    }

    /** 解析同目录下的唯一名称（冲突自动加序号后缀，规则见 FileUtil）。 */
    @Override
    public String resolveUniqueName(Long userId, Long parentId, String baseName) {
        return FileUtil.resolveUniqueName(fileMapper, userId, parentId, baseName);
    }

    /** 构建 path（父目录 path + "/" + name），根目录下为 "/name" */
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

    /** 递归收集子树（含根节点），使用 MySQL 8 CTE 替代全表扫描 */
    private List<File> collectSubtree(Long userId, Long rootId) {
        return fileMapper.findSubtree(rootId, userId);
    }
}
