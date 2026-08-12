package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 合并分片请求。
 *
 * 修改指引：
 * - 【统一】修改 uploadId        → String uploadId；对应 POST /api/files/upload/merge 入参，取自 upload/init 返回值；
 *                         服务端校验归属与分片完整性，缺失分片/哈希不符会合并失败；改名需同步前端合并流程与 UploadServiceImpl 会话校验
 * - 【统一】修改校验注解 @NotBlank → 空 uploadId 直接 400，改动影响接口契约；改后需同步前端必填契约
 */
@Data
public class UploadMergeRequest {

    @NotBlank(message = "uploadId 不能为空")
    private String uploadId;
}
