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
 */
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

    int deleteByIds(@Param("ids") List<Long> ids);

    List<File> findAll();
}
