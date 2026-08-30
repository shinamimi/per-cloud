package com.cloud.backend.enums;

import lombok.Getter;

@Getter
    public enum CaptchaType {

        /** 【统一】改后需同步验证码发送/校验逻辑（Redis Key 拼接、场景分发） */
        REGISTER,
    RESET_PASSWORD,
    LOGIN;
}