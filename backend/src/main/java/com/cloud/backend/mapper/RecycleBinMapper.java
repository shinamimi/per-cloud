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

    /* ==================== 团队回收站（docs/team-module.md §六） ==================== */

    /** 团队回收站记录（倒序） */
    List<RecycleBin> findByTeamId(Long teamId);

    /** 团队回收站单条（恢复/物理删除用） */
    RecycleBin findByIdAndTeamId(@Param("id") Long id, @Param("teamId") Long teamId);

    /** 团队目录下的子回收记录（物理清理团队目录时递归处理） */
    List<RecycleBin> findByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    /** 团队级联删除：清理以该目录为父节点的子记录 */
    int deleteByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    /* ==================== 全局回收站（管理员删除，仅 ADMIN 可见，docs/adr/012） ==================== */

    /** 全局回收站记录（deleted_by=1，倒序） */
    List<RecycleBin> findGlobal();

    /** 全局回收站单条（恢复/物理删除用） */
    RecycleBin findGlobalById(Long id);

    /** 全局回收站中某目录下的子记录（管理员恢复目录时递归处理） */
    List<RecycleBin> findGlobalChildrenByUserId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    List<RecycleBin> findGlobalChildrenByTeamId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);
}
