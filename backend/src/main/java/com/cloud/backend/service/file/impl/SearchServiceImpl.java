package com.cloud.backend.service.file.impl;

import com.cloud.backend.dao.FileDao;
import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.service.file.SearchService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    private final FileDao fileDao;

    public SearchServiceImpl(FileDao fileDao) {
        this.fileDao = fileDao;
    }

    @Override
    public Page<FileNodeResponse> search(Long userId, String keyword, Integer category, int page, int size) {
        FileQuery query = new FileQuery();
        query.setUserId(userId);
        query.setKeyword(keyword == null ? "" : keyword.trim());
        query.setCategory(category);
        query.setOffset((page - 1) * size);
        query.setSize(size);
        long total = fileDao.count(query);
        List<FileNodeResponse> records = fileDao.searchPage(query).stream()
                .map(FileNodeResponse::from)
                .toList();
        return new Page<>(records, total, page, size);
    }
}
