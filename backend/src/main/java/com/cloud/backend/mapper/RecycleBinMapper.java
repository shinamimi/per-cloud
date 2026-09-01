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
 *
 * 修改指引：
 * - 【习惯】写入删除记录          → insert（XML：src/main/resources/mapper/RecycleBinMapper.xml）；删除时由 Service 同步写入、
 *                          恢复时反向操作，改字段（如 file_hash/type/team_id）需同步 XML 与实体
 * - 【习惯】个人回收站            → findByIdAndUserId / findByUserId / findByUserIdAndParentId（XML 同上）；
 *                          条件含 team_id=0 且 deleted_by=0，改归属判定需与 Service 的删除来源（deleted_by）联动
 * - 【习惯】团队回收站            → findByTeamId / findByIdAndTeamId / findByTeamIdAndParentId（XML 同上）；
 *                          按 team_id 过滤且 deleted_by=0，团队解散清理需同步 Service 层
 * - 【习惯】全局回收站（管理员删除）→ findGlobal / findGlobalById / findGlobalChildrenByUserId / findGlobalChildrenByTeamId（XML 同上）；
 *                          deleted_by=1 且仅 ADMIN 可见，改可见性需同步 AdminFileService 权限与
 *                          DDL 索引 idx_deleted_by(deleted_by, team_id)
 * - 【习惯】定时物理清理          → findByExpireTimeBefore（XML 同上）；按 expire_time 扫描过期记录，
 *                          配合 deleteById 清理，改保留天数需同步定时任务配置
 * - 【习惯】级联清理子记录         → deleteByUserIdAndParentId / deleteByTeamIdAndParentId（XML 同上）；按原父目录（parent_id）
 *                          递归清理，恢复/清理目录时由 Service 调用，改递归规则需同步 XML
 */
@Mapper
public interface RecycleBinMapper {

    int insert(RecycleBin recycleBin);

    /** 批量插入回收站记录（替代循环单条 INSERT） */
    int batchInsert(@Param("records") List<RecycleBin> records);

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

    /* ==================== 团队回收站 ==================== */

    /** 团队回收站记录（倒序） */
    List<RecycleBin> findByTeamId(Long teamId);

    /** 团队回收站单条（恢复/物理删除用） */
    RecycleBin findByIdAndTeamId(@Param("id") Long id, @Param("teamId") Long teamId);

    /** 团队目录下的子回收记录（物理清理团队目录时递归处理） */
    List<RecycleBin> findByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    /** 团队级联删除：清理以该目录为父节点的子记录 */
    int deleteByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    /* ==================== 全局回收站（管理员删除，仅 ADMIN 可见） ==================== */

    /** 全局回收站记录（deleted_by=1，倒序） */
    List<RecycleBin> findGlobal();

    /** 全局回收站单条（恢复/物理删除用） */
    RecycleBin findGlobalById(Long id);

    /** 全局回收站中某目录下的子记录（管理员恢复目录时递归处理） */
    List<RecycleBin> findGlobalChildrenByUserId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    List<RecycleBin> findGlobalChildrenByTeamId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);
}
