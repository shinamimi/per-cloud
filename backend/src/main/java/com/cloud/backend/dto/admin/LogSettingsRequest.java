package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class LogSettingsRequest {

    /** 操作日志保存天数 */
    private Integer operationDays;

    /** 登录日志保存天数 */
    private Integer loginDays;
}
