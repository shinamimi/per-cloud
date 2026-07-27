package com.cloud.backend.mapper;

import com.cloud.backend.entity.RecycleBin;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 回收站 Mapper —— 记录用户已删除的文件，支持恢复。
 */
@Mapper
public interface RecycleBinMapper {

    int insert(RecycleBin recycleBin);

    int deleteById(Long id);

    List<RecycleBin> findByUserId(Long userId);
}