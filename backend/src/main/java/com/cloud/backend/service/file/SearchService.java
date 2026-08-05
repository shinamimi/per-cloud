package com.cloud.backend.service.file;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.FileNodeResponse;

/**
 * 文件搜索服务 —— 文件名 LIKE + 类型过滤（分类），后续可扩展 ES 全文检索。
 *
 * 修改指引：
 * - 【习惯】想改"搜索匹配规则（文件名 LIKE、是否含扩展名/路径）" → search() 对应 SearchServiceImpl 组装 FileQuery
 *   与 FileDao.searchPage/count 的 SQL（keyword 去空格、category 可空）；改动影响命中范围与分页总数
 * - 【习惯】想改"分类过滤来源" → category 与 FileConstants 分类常量（如 AUDIO）；改动须与上传时分类打标逻辑联动
 * - 【习惯】想改"搜索可见范围（如加入团队空间/回收站）" → FileQuery.userId 维度与 FileDao SQL；改动影响搜索结果
 *   的归属边界
 * - 【习惯】想改"升级全文检索（如 ES）" → 替换 search() 的查询组装与底层 FileDao 实现；改动影响搜索语义与依赖
 * - 【习惯】新增方法 → 需同步实现类 SearchServiceImpl 与 FileController 调用方
 */
public interface SearchService {

    Page<FileNodeResponse> search(Long userId, String keyword, Integer category, int page, int size);
}
