package com.cloud.backend.mapper;

import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FileMapper {

    int insert(File file);

    File findById(Long id);

    List<File> findByUserIdAndParentId(Long userId, Long parentId);

    File findByUserIdAndPath(Long userId, String path);

    int update(File file);

    int deleteById(Long id);

    int updateStatus(Long id, Integer status);
}