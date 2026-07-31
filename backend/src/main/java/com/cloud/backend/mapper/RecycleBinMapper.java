package com.cloud.backend.mapper;

import com.cloud.backend.entity.RecycleBin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站 Mapper —— t_recycle_bin 表。
 * 删除时插入记录（保留原文件信息与过期时间），恢复时删除记录，
 * 定时任务按 expire_time 扫描物理清理。
 */
@Mapper
public interface RecycleBinMapper {

    int insert(RecycleBin recycleBin);

    RecycleBin findById(Long id);

    RecycleBin findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<RecycleBin> findByUserId(Long userId);

    /** 已过期的记录（定时任务物理清理） */
    List<RecycleBin> findByExpireTimeBefore(LocalDateTime time);

    /** 某目录下的子回收记录（物理清理目录时递归处理） */
    List<RecycleBin> findByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    int deleteById(Long id);

    /** 级联删除：物理清理目录时，同时清理以该目录为父节点的子记录 */
    int deleteByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);
}
