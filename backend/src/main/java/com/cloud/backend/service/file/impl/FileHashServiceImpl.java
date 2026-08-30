package com.cloud.backend.service.file.impl;

import com.cloud.backend.entity.FileHash;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.service.file.FileHashService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class FileHashServiceImpl implements FileHashService {

    private final FileHashMapper fileHashMapper;

    public FileHashServiceImpl(FileHashMapper fileHashMapper) {
        this.fileHashMapper = fileHashMapper;
    }

    @Override
    public String register(String fileHash, String objectName, long size, String mimeType) {
        FileHash existing = fileHashMapper.findByHash(fileHash);
        if (existing != null) {
            fileHashMapper.incrementRefCount(fileHash);
            return existing.getObjectName();
        }
        FileHash newHash = new FileHash();
        newHash.setFileHash(fileHash);
        newHash.setObjectName(objectName);
        newHash.setSize(size);
        newHash.setMimeType(mimeType);
        newHash.setRefCount(1);
        try {
            fileHashMapper.insert(newHash);
            return objectName;
        } catch (DuplicateKeyException e) {
            // 并发注册：另一请求已建索引 → 共享其对象
            FileHash concurrent = fileHashMapper.findByHash(fileHash);
            fileHashMapper.incrementRefCount(fileHash);
            return concurrent.getObjectName();
        }
    }

    @Override
    public void shareRef(String fileHash) {
        fileHashMapper.incrementRefCount(fileHash);
    }

    @Override
    public boolean releaseRef(String fileHash) {
        int updated = fileHashMapper.decrementRefCount(fileHash);
        if (updated == 0) {
            return false;
        }
        FileHash after = fileHashMapper.findByHash(fileHash);
        if (after == null || after.getRefCount() <= 0) {
            fileHashMapper.deleteByHash(fileHash);
            return true;
        }
        return false;
    }
}
