package com.cloud.backend.service.file.impl;

import com.cloud.backend.dao.FileDao;
import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.service.file.SearchService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件搜索服务实现（文件名 LIKE + 分类过滤，个人空间）。
 *
 * 修改指引：
 * - 【习惯】想改"搜索匹配规则（文件名 LIKE、是否含扩展名/路径）" → search() 组装 FileQuery 与 FileDao.searchPage/count
 *   的 SQL（keyword 去空格、category 可空）；改动影响命中范围与分页总数
 * - 【习惯】想改"分类过滤来源" → FileQuery.category 与 FileConstants 分类常量（如 AUDIO）；改动须与上传时
 *   分类打标逻辑联动
 * - 【习惯】想改"搜索可见范围（如加入团队空间/回收站）" → FileQuery.userId 维度与 FileDao SQL；改动影响搜索结果的
 *   归属边界
 * - 【习惯】与接口联动：本类实现 SearchService，改签名/行为须同步接口契约及 FileController 调用方
 */
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
