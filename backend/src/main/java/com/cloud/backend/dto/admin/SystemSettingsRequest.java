package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class SystemSettingsRequest {

    private Boolean allowRegister;
    private Boolean allowGuestShare;
    private Boolean enableMailVerify;
    private Boolean enableCaptcha;
    private Boolean enableOperationLog;
}
