package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 上传限制更新请求（null 字段保持原值）。
 */
@Data
public class AdminUploadLimitsRequest {

    private Long maxSizeUser;
    private Long maxSizeVip;
    private Integer maxConcurrentUser;
    private Integer maxConcurrentVip;
}
