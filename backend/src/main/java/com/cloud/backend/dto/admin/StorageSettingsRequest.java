package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 存储限制更新请求（null 字段恢复配置默认值）。
 * 只影响以后新注册用户，不影响存量用户。
 *
 * 修改指引：
 * - 【统一】修改 defaultQuotaUser/defaultQuotaVip → Long 新用户/新 VIP 默认配额（单位字节）；改动只影响新注册用户，不影响存量用户
 * - 【统一】修改单位             → 字节，前端需换算展示；改动需同步配额换算与展示
 * - 【统一】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 */
@Data
public class StorageSettingsRequest {

    private Long defaultQuotaUser;
    private Long defaultQuotaVip;
}
