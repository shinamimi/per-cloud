package com.cloud.backend.enums;

import lombok.Getter;

/**
 * 验证码用途类型 —— 区分不同场景下的验证码，防止串用。
 * 存储时拼入 Redis Key（captcha:REGISTER:email / captcha:RESET_PASSWORD:email），
 * 不同场景的验证码即使发给同一个邮箱也不会互相覆盖。
 *
 * 修改指引：
 * - 【习惯】新增枚举值          → 在枚举末尾追加常量；需同步在验证码发送/校验逻辑（Redis Key 拼接、场景分发）中
 *                          处理新场景，否则新类型发不出或校验不到
 * - 【习惯】重命名枚举值        → 修改常量名；会改变 Redis Key（captcha:xxx:email）与前端请求参数（SendCodeRequest），
 *                          存量验证码按旧 Key 无法再校验
 * - 【习惯】删除枚举值          → 删除常量并清理引用；已发送的验证码按旧 Key 无法校验，属废弃预期
 */
@Getter
public enum CaptchaType {

    REGISTER,
    RESET_PASSWORD,
    LOGIN;
}