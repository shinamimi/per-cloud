package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 存储限制更新请求（null 字段恢复配置默认值）。
 * 只影响以后新注册用户，不影响存量用户。
 */
@Data
public class StorageSettingsRequest {

    private Long defaultQuotaUser;
    private Long defaultQuotaVip;
}
