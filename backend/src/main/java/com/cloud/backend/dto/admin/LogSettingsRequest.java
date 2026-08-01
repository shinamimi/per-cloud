package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 日志配置更新请求（null 字段恢复配置默认值）。
 */
@Data
public class LogSettingsRequest {

    /** 操作日志保存天数 */
    private Integer operationDays;

    /** 登录日志保存天数 */
    private Integer loginDays;
}
