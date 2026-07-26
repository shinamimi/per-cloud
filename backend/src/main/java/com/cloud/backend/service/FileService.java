package com.cloud.backend.service;

import com.cloud.backend.entity.File;

import java.util.List;

public interface FileService {

    File save(File file);

    File findById(Long id);

    List<File> listByUserAndParent(Long userId, Long parentId);

    File findByPath(Long userId, String path);

    int update(File file);

    int removeById(Long id);

    int updateStatus(Long id, Integer status);

    List<File> findAll();
}