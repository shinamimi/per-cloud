package com.cloud.backend.service.impl;

import com.cloud.backend.entity.File;
import com.cloud.backend.mapper.FileMapper;
import com.cloud.backend.service.FileService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;

    public FileServiceImpl(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public File save(File file) {
        fileMapper.insert(file);
        return file;
    }

    @Override
    public File findById(Long id) {
        return fileMapper.findById(id);
    }

    @Override
    public List<File> listByUserAndParent(Long userId, Long parentId) {
        return fileMapper.findByUserIdAndParentId(userId, parentId);
    }

    @Override
    public File findByPath(Long userId, String path) {
        return fileMapper.findByUserIdAndPath(userId, path);
    }

    @Override
    public int update(File file) {
        return fileMapper.update(file);
    }

    @Override
    public int removeById(Long id) {
        return fileMapper.deleteById(id);
    }

    @Override
    public int updateStatus(Long id, Integer status) {
        return fileMapper.updateStatus(id, status);
    }

    @Override
    public List<File> findAll() {
        return fileMapper.findAll();
    }
}