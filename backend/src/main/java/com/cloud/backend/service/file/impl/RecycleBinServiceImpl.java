package com.cloud.backend.service.file.impl;

import com.cloud.backend.entity.RecycleBin;
import com.cloud.backend.mapper.RecycleBinMapper;
import com.cloud.backend.service.file.RecycleBinService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 回收站服务实现。
 */
@Service
public class RecycleBinServiceImpl implements RecycleBinService {

    private final RecycleBinMapper recycleBinMapper;

    public RecycleBinServiceImpl(RecycleBinMapper recycleBinMapper) {
        this.recycleBinMapper = recycleBinMapper;
    }

    @Override
    public RecycleBin save(RecycleBin recycleBin) {
        recycleBinMapper.insert(recycleBin);
        return recycleBin;
    }

    @Override
    public int removeById(Long id) {
        return recycleBinMapper.deleteById(id);
    }

    @Override
    public List<RecycleBin> listByUserId(Long userId) {
        return recycleBinMapper.findByUserId(userId);
    }
}