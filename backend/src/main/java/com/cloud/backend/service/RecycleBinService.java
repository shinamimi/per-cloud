package com.cloud.backend.service;

import com.cloud.backend.entity.RecycleBin;

import java.util.List;

public interface RecycleBinService {

    RecycleBin save(RecycleBin recycleBin);

    int removeById(Long id);

    List<RecycleBin> listByUserId(Long userId);
}