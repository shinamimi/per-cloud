package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量打包下载请求。
 *
 * 修改指引：
 * - 【统一】修改 fileIds         → List&lt;Long&gt; fileIds；批量打包的文件/目录 id 列表，请求体字段名对应
 *                         POST /api/files/download/batch 入参；支持传目录（服务层递归展开其下所有文件）；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改校验注解 @NotEmpty → 当前空列表直接 400；改动后影响接口契约，前端需保证至少勾选一个文件；改后需同步前端勾选校验
 */
@Data
public class BatchDownloadRequest {

    @NotEmpty(message = "请选择要下载的文件")
    private List<Long> fileIds;
}
