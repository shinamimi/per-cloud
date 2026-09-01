package com.cloud.backend.mapper;

import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件 Mapper。
 *
 * 核心方法：
 * - findByUserIdAndParentId：按用户和父目录 ID 列出子文件和目录，用于"打开文件夹"场景
 * - pageByUserIdAndParentId / countByUserIdAndParentId：分页列表
 * - findByUserIdAndParentIdAndName：同名检查（重名自动加后缀时使用）
 * - findByUserId：用户全部正常文件（目录树、递归删除收集）
 * - updateStatusByIds：批量逻辑删除（递归删除）
 * - deleteByIds：物理删除（回收站清理）
 *
 * 修改指引：
 * - 【习惯】增删文件             → insert / deleteById / deleteByIds（XML：src/main/resources/mapper/FileMapper.xml）；
 *                          deleteByIds 为 IN 批量物理删除（回收站清理用），改字段名需同步 XML 与实体；
 *                          insert 的 name 参与唯一索引 uk_user_parent_name(user_id, parent_id, name, team_id)，
 *                          改 name 或空间维度字段需同步数据库 DDL
 * - 【习惯】按用户/目录查询       → findByUserIdAndParentId（XML 同上）；仅 status != 0 的正常文件，按 type DESC, name ASC 排序，
 *                          改 status 过滤条件需与 Service 层删除/禁用状态联动
 * - 【习惯】分页查询（用户/团队）  → pageByUserIdAndParentId / pageByTeamIdAndParentId 及配套 count（XML 同上）；
 *                          LIMIT #{offset}, #{size} 由 Service 传入，改排序或返回列需同步 XML
 * - 【习惯】同名检查             → findByUserIdAndParentIdAndName / findByTeamIdAndParentIdAndName（XML 同上）；
 *                          重名自动加后缀时使用，仅 status != 0，配合唯一索引 uk_user_parent_name 防并发竞态
 * - 【习惯】按路径查询            → findByUserIdAndPath（XML 同上）；按完整路径查唯一文件，改 path 维护逻辑需同步 Service 层
 * - 【习惯】更新文件             → update / updateName / updateParent / updateStatus / updateStatusByIds（XML 同上）；
 *                          updateName 仅改 name（重命名）、updateParent 仅改 parentId（移动，MinIO 对象不动），
 *                          updateStatusByIds 为批量逻辑删除（递归删除收集的 ids），status 语义（0=删除/1=正常/2=禁用）
 *                          与 Service 层删除流程联动
 * - 【习惯】对象级禁用/恢复       → disableByHash / disableByHashAndUser / restoreByHash（XML 同上）；
 *                          SQL 条件 status 1→2 且 is_directory=0，改禁用状态值需与 DisabledObject 的 scope 规则
 *                          及 AdminFileService 启用流程联动
 * - 【习惯】团队维度查询          → findByTeamIdAndParentId / findByTeamId 等（XML 同上）；同表以 team_id 区分个人（0）/团队（>0）
 *                          空间，改归属判断需同步 SQL 与 DDL 索引 idx_team(team_id, parent_id, status)
 * - 【习惯】管理端全局文件        → adminPage / adminCount（XML 同上）；adminWhere 动态 SQL 含 username 子查询与排序分支，
 *                          改过滤/排序需同步 XML，且保持分页 offset/size 入参语义
 */
@Mapper
public interface FileMapper {

    int insert(File file);

    File findById(Long id);

    /** 批量查询文件（IN 查询，替代 N+1） */
    List<File> findByIds(@Param("ids") java.util.Collection<Long> ids);

    List<File> findByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    List<File> pageByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId,
                                       @Param("offset") int offset, @Param("size") int size);

    long countByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    File findByUserIdAndParentIdAndName(@Param("userId") Long userId, @Param("parentId") Long parentId,
                                        @Param("name") String name);

    File findByUserIdAndPath(@Param("userId") Long userId, @Param("path") String path);

    List<File> findByUserId(Long userId);

    /** 递归查询子树（MySQL 8 CTE，替代全表扫描） */
    List<File> findSubtree(@Param("rootId") Long rootId, @Param("userId") Long userId);

    int update(File file);

    int updateName(@Param("id") Long id, @Param("name") String name);

    int updateParent(@Param("id") Long id, @Param("parentId") Long parentId);

    int deleteById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /* ==================== 对象级禁用（按内容 hash） ==================== */

    /** 按内容 hash 禁用全部引用（全站禁） */
    int disableByHash(String fileHash);

    /** 按内容 hash 禁用指定用户引用（仅用户禁） */
    int disableByHashAndUser(@Param("fileHash") String fileHash, @Param("userId") Long userId);

    /** 按内容 hash 恢复全部引用（启用时先统一恢复，再重放剩余禁用记录） */
    int restoreByHash(String fileHash);

    int deleteByIds(@Param("ids") List<Long> ids);

    List<File> findAll();

    /* ==================== 团队维度（同表 + teamId） ==================== */

    List<File> findByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    List<File> pageByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId,
                                       @Param("offset") int offset, @Param("size") int size);

    long countByTeamIdAndParentId(@Param("teamId") Long teamId, @Param("parentId") Long parentId);

    /** 团队空间同名检查（同团队同目录 name 唯一，跨 user_id 共享命名空间） */
    File findByTeamIdAndParentIdAndName(@Param("teamId") Long teamId, @Param("parentId") Long parentId,
                                        @Param("name") String name);

    /** 团队全部正常文件（目录树、递归子树收集） */
    List<File> findByTeamId(Long teamId);

    /* ==================== 管理端全局文件（仅 ADMIN） ==================== */

    List<File> adminPage(com.cloud.backend.dto.AdminFileQuery query);

    long adminCount(com.cloud.backend.dto.AdminFileQuery query);

    /** 统计文件总数（替代 findAll().size()） */
    long countAll();

    /** 统计文件总大小（替代 findAll().stream().mapToLong().sum()） */
    long sumSize();
}
