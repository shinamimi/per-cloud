package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.entity.FileHash;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.mapper.RecycleBinMapper;
import com.cloud.backend.service.file.FileHashService;
import com.cloud.backend.service.file.RecycleBinService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.system.OperationLogService;
import com.cloud.backend.service.user.UserService;
import com.cloud.backend.utils.FileUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站服务实现。
 * 物理删除对象条件：秒传对象引用计数归零才删（与 30 天清理联动）；普通对象直接删。
 *
 * 修改指引：
 * - 【习惯】想改"恢复流程（递归恢复子树 + 配额校验 + 同名唯一化 + 父目录可用校验）" → restore()/restoreRecord()；
 *   改动影响恢复后目录结构完整性与配额是否超限
 * - 【习惯】想改"物理删除条件（引用归零才删）" → releaseObject()：有 hash 走 fileHashService.releaseRef() 归零判定，
 *   无 hash 直接删 MinIO 对象；改动影响秒传共享对象是否被误删
 * - 【习惯】想改"过期清理" → purgeExpired()（findByExpireTimeBefore + purgeInternal，由定时任务调用）；
 *   改动影响过期记录的物理删除时机与保留策略
 * - 【习惯】想改"递归清理维度" → purgeInternal()：目录按团队维度（findByTeamIdAndParentId）或个人维度
 *   （findByUserIdAndParentId）递归子记录；改动影响跨成员上传的团队目录清理
 * - 【习惯】幂等说明：purgeInternal() 先按 id 复查记录存在性，避免递归处理中重复删除；
 *   改动清理入口须保持该幂等防护
 * - 【习惯】操作日志：restoreRecord() 内联写 RESTORE_FILE 操作日志；改动影响 OperationLogService
 * - 【习惯】事务说明：purgeExpired()/purgeRecord() 无显式 @Transactional，引用计数与存储删除依赖 FileHashMapper/
 *   StorageService 各自的原子操作；若需整体回滚须补充事务边界
 * - 【习惯】与接口联动：本类实现 RecycleBinService，改签名/行为须同步接口契约及 FileController、
 *   AdminFileServiceImpl（purge 复用 purgeRecord）等调用方
 */
@Service
public class RecycleBinServiceImpl implements RecycleBinService {

    private final RecycleBinMapper recycleBinMapper;
    private final FileMapper fileMapper;
    private final FileHashMapper fileHashMapper;
    private final FileHashService fileHashService;
    private final StorageService storageService;
    private final UserService userService;
    private final OperationLogService operationLogService;
    private final FileProperties fileProperties;

    public RecycleBinServiceImpl(RecycleBinMapper recycleBinMapper, FileMapper fileMapper,
                                 FileHashMapper fileHashMapper, FileHashService fileHashService,
                                 StorageService storageService, UserService userService,
                                 OperationLogService operationLogService, FileProperties fileProperties) {
        this.recycleBinMapper = recycleBinMapper;
        this.fileMapper = fileMapper;
        this.fileHashMapper = fileHashMapper;
        this.fileHashService = fileHashService;
        this.storageService = storageService;
        this.userService = userService;
        this.operationLogService = operationLogService;
        this.fileProperties = fileProperties;
    }

    @Override
    public RecycleBin save(RecycleBin recycleBin) {
        recycleBinMapper.insert(recycleBin);
        return recycleBin;
    }

    @Override
    public int removeById(Long id) {
        return recycleBinMapper.deleteById(id);
    }

    @Override
    public List<RecycleBin> listByUserId(Long userId) {
        return recycleBinMapper.findByUserId(userId);
    }

    @Override
    public void restore(Long userId, Long recycleId) {
        RecycleBin record = recycleBinMapper.findByIdAndUserId(recycleId, userId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        restoreRecord(record);
    }

    /**
     * 递归恢复单条记录：先恢复父（占配额）再恢复子。
     * 删除时子树节点 status 一并置 DELETED 且每节点都有回收站记录，
     * 只恢复顶层会让目录内容丢失，故须递归（子记录 parentId 仍指向原父目录 id）。
     */
    private void restoreRecord(RecycleBin record) {
        com.cloud.backend.entity.File file = fileMapper.findById(record.getFileId());
        if (file == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND, "原始文件记录不存在");
        }
        if (file.getParentId() != null && file.getParentId() != FileConstants.ROOT_PARENT_ID) {
            com.cloud.backend.entity.File parent = fileMapper.findById(file.getParentId());
            if (parent == null || parent.getStatus() == FileStatus.DELETED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父目录不可用，请先恢复父目录");
            }
        }
        if (record.getSize() > 0) {
            long remaining = userService.getRemainingQuota(record.getUserId());
            if (record.getSize() > remaining) {
                throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED, "恢复后空间不足，请先清理其他文件");
            }
            userService.changeUsedSpace(record.getUserId(), record.getSize());
        }
        // 删除时顶层节点已改内部名，此处还原唯一名（若同名已被占用则自动追加后缀）
        String uniqueName = FileUtil.resolveUniqueName(fileMapper, record.getUserId(), file.getParentId(), record.getOriginalName());
        if (!uniqueName.equals(file.getName())) {
            fileMapper.updateName(record.getFileId(), uniqueName);
        }
        fileMapper.updateStatus(record.getFileId(), FileStatus.NORMAL.getValue());
        recycleBinMapper.deleteById(record.getId());

        if (record.getType() != null && record.getType() == 1) {
            List<RecycleBin> children = recycleBinMapper.findByUserIdAndParentId(record.getUserId(), record.getFileId());
            for (RecycleBin child : children) {
                restoreRecord(child);
            }
        }

        OperationLog log = new OperationLog();
        log.setUserId(record.getUserId());
        log.setOperation(OperationType.RESTORE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(record.getFileId());
        log.setDetail("恢复文件: " + record.getOriginalName());
        operationLogService.log(log);
    }

    @Override
    public void purge(Long userId, Long recycleId) {
        RecycleBin record = recycleBinMapper.findByIdAndUserId(recycleId, userId);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECYCLE_NOT_FOUND);
        }
        purgeInternal(record);
    }

    @Override
    public void purgeRecord(RecycleBin record) {
        if (record != null) {
            purgeInternal(record);
        }
    }

    @Override
    public void purgeExpired() {
        List<RecycleBin> expired = recycleBinMapper.findByExpireTimeBefore(LocalDateTime.now());
        for (RecycleBin record : expired) {
            purgeInternal(record);
        }
    }

    /**
     * 物理删除单个回收记录：目录递归处理子记录；释放 MinIO 对象（引用归零时）；删除 t_file 与回收站记录。
     */
    private void purgeInternal(RecycleBin record) {
        // 幂等防护：记录可能已被递归处理删除
        RecycleBin latest = recycleBinMapper.findById(record.getId());
        if (latest == null) {
            return;
        }
        if (latest.getType() != null && latest.getType() == 1) {
            // 团队目录的子记录可能由不同成员上传（userId 不同），须按团队维度递归
            if (latest.getTeamId() != null && latest.getTeamId() > 0) {
                List<RecycleBin> children = recycleBinMapper.findByTeamIdAndParentId(latest.getTeamId(), latest.getFileId());
                for (RecycleBin child : children) {
                    purgeInternal(child);
                }
            } else {
                List<RecycleBin> children = recycleBinMapper.findByUserIdAndParentId(latest.getUserId(), latest.getFileId());
                for (RecycleBin child : children) {
                    purgeInternal(child);
                }
            }
        }
        releaseObject(latest);
        fileMapper.deleteById(latest.getFileId());
        recycleBinMapper.deleteById(latest.getId());
    }

    /** 释放物理对象：秒传对象引用归零才删除；无哈希对象直接删除 */
    private void releaseObject(RecycleBin record) {
        String hash = record.getFileHash();
        if (hash != null && !hash.isEmpty()) {
            FileHash fileHash = fileHashMapper.findByHash(hash);
            if (fileHash == null) {
                return;
            }
            boolean zero = fileHashService.releaseRef(hash);
            if (zero && fileHash.getObjectName() != null && !fileHash.getObjectName().isEmpty()) {
                storageService.delete(fileHash.getObjectName());
            }
            return;
        }
        String objectName = record.getObjectName();
        if (objectName != null && !objectName.isEmpty()) {
            storageService.delete(objectName);
        }
    }
}
