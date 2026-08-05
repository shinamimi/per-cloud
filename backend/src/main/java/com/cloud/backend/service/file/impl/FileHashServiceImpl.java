package com.cloud.backend.service.file.impl;

import com.cloud.backend.entity.FileHash;
import com.cloud.backend.mapper.FileHashMapper;
import com.cloud.backend.service.file.FileHashService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 秒传索引服务实现。
 * 全局 SHA256 索引：命中即共享物理对象（引用计数 +1），
 * 物理删除时引用归零才真正删除 MinIO 对象。
 *
 * 修改指引：
 * - 【习惯】想改"秒传命中判定/共享对象策略" → register()：命中返回既有 objectName，未命中新建索引并回退（并发注册
 *   DuplicateKeyException 时共享并发方对象）；改动影响去重生效范围与并发一致性
 * - 【习惯】想改"引用计数增减时机" → register() 命中/并发回退 +1、shareRef() 分享创建 +1、releaseRef() 物理删除 -1；
 *   改动影响 MinIO 对象何时真正被删（归零才删）
 * - 【习惯】想改"引用归零判定与清理" → releaseRef() 的 decrementRefCount 返回值判定与 deleteByHash()；
 *   改动影响物理对象的删除与复用
 * - 【习惯】并发注意：引用计数依赖 file_hash.ref_count 的原子 SQL（incrementRefCount/decrementRefCount），
 *   勿改成"先查后改"的读改写，否则并发上传/删除会错乱
 * - 【习惯】与接口联动：本类实现 FileHashService，改签名/行为须同步接口契约及 UploadServiceImpl/RecycleBinServiceImpl
 *   （purgeRecord）/AdminFileServiceImpl 等调用方
 */
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
