package com.cloud.backend.mapper;

import com.cloud.backend.entity.FileHash;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 秒传索引 Mapper —— t_file_hash 表。
 * findByHash 命中即秒传；refCount 引用计数，归零才物理删除对象。
 */
@Mapper
public interface FileHashMapper {

    int insert(FileHash fileHash);

    FileHash findByHash(String fileHash);

    int incrementRefCount(String fileHash);

    int decrementRefCount(String fileHash);

    int deleteByHash(String fileHash);
}
