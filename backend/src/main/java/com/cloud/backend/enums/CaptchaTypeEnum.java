package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 验证码用途类型 —— 区分不同场景下的验证码，防止串用。
 * 存储时拼入 Redis Key（captcha:REGISTER:email / captcha:RESET_PASSWORD:email），
 * 不同场景的验证码即使发给同一个邮箱也不会互相覆盖。
 */
@Getter
public enum CaptchaTypeEnum {

    REGISTER,
    RESET_PASSWORD,
    LOGIN;
}