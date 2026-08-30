package com.cloud.backend.mapper;

import com.cloud.backend.entity.FileHash;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileHashMapper {

    int insert(FileHash fileHash);

    FileHash findByHash(String fileHash);

    int incrementRefCount(String fileHash);

    int decrementRefCount(String fileHash);

    int deleteByHash(String fileHash);
}
