package com.cloud.backend.mapper;

import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 文件 Mapper。
 *
 * 核心方法：
 * - findByUserIdAndParentId：按用户和父目录 ID 列出子文件和目录，用于"打开文件夹"场景
 * - findByUserIdAndPath：按用户和路径查找，用于校验路径唯一性
 * - updateStatus：逻辑删除时更新 FileStatus
 */
@Mapper
public interface FileMapper {

    int insert(File file);

    File findById(Long id);

    List<File> findByUserIdAndParentId(Long userId, Long parentId);

    File findByUserIdAndPath(Long userId, String path);

    int update(File file);

    int deleteById(Long id);

    int updateStatus(Long id, Integer status);

    List<File> search(FileQuery query);

    List<File> findAll();
}