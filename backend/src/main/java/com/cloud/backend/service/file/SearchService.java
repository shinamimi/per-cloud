package com.cloud.backend.service.file;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.FileNodeResponse;

/**
 * 文件搜索服务 —— 文件名 LIKE + 类型过滤（分类），后续可扩展 ES 全文检索。
 */
public interface SearchService {

    Page<FileNodeResponse> search(Long userId, String keyword, Integer category, int page, int size);
}
