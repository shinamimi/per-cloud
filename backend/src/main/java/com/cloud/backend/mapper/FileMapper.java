package com.cloud.backend.mapper;

import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    int insert(File file);

    File findById(Long id);

    List<File> findByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    List<File> pageByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId,
                                       @Param("offset") int offset, @Param("size") int size);

    long countByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    File findByUserIdAndParentIdAndName(@Param("userId") Long userId, @Param("parentId") Long parentId,
                                        @Param("name") String name);

    File findByUserIdAndPath(@Param("userId") Long userId, @Param("path") String path);

    List<File> findByUserId(Long userId);

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
}
