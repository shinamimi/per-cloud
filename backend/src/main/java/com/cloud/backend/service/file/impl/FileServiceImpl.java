package com.cloud.backend.service.file.impl;

import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.entity.File;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.security.LoginUser;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final StorageService storageService;
    private final OperationLogService operationLogService;

    public FileServiceImpl(FileMapper fileMapper, StorageService storageService,
                           OperationLogService operationLogService) {
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.operationLogService = operationLogService;
    }

    @Override
    public File save(File file) {
        fileMapper.insert(file);
        return file;
    }

    @Override
    public File findById(Long id) {
        return fileMapper.findById(id);
    }

    @Override
    public List<File> listByUserAndParent(Long userId, Long parentId) {
        return fileMapper.findByUserIdAndParentId(userId, parentId);
    }

    @Override
    public File findByPath(Long userId, String path) {
        return fileMapper.findByUserIdAndPath(userId, path);
    }

    @Override
    public int update(File file) {
        return fileMapper.update(file);
    }

    @Override
    public int removeById(Long id) {
        return fileMapper.deleteById(id);
    }

    @Override
    public int updateStatus(Long id, Integer status) {
        return fileMapper.updateStatus(id, status);
    }

    @Override
    public List<File> search(FileQuery query) {
        return fileMapper.search(query);
    }

    @Override
    public List<File> findAll() {
        return fileMapper.findAll();
    }

    @Override
    public void adminDeleteFile(Long id) {
        File file = fileMapper.findById(id);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (file.getObjectName() != null && !file.getObjectName().isEmpty()) {
            storageService.delete(file.getObjectName());
        }
        fileMapper.deleteById(id);

        OperationLog log = new OperationLog();
        log.setUserId(getCurrentUserId());
        log.setOperation(OperationType.DELETE_FILE);
        log.setTargetType(TargetType.FILE);
        log.setTargetId(id);
        log.setDetail("管理员删除文件: " + file.getName());
        operationLogService.log(log);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }
}
