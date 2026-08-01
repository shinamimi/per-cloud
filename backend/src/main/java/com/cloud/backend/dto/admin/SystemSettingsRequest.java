package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 系统功能开关更新请求（null 字段恢复配置默认值）。仅 ADMIN 可保存。
 */
@Data
public class SystemSettingsRequest {

    private Boolean allowRegister;
    private Boolean allowGuestShare;
    private Boolean enableMailVerify;
    private Boolean enableCaptcha;
    private Boolean enableOperationLog;
}
