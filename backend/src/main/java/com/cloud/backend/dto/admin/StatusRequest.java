package com.cloud.backend.dto.admin;

import com.cloud.backend.enums.UserStatus;
import lombok.Data;

/**
 * 用户状态调整请求 DTO —— 后台启用/禁用/锁定用户。
 *
 * 修改指引：
 * - 【统一】修改 status           → 自定义枚举 UserStatus（enums/UserStatus.java：DISABLED=0 禁用/NORMAL=1 正常/LOCKED=2 锁定/
 *                           INACTIVE=3 未激活），存储 TINYINT，LoginUser.isEnabled() 基于此判断；
 *                           改动需同步枚举定义与登录启用逻辑、前端状态展示
 * - 【统一】修改字段名/类型        → 对应用户状态调整接口请求参数，改动需同步 service 与前端
 */
@Data
public class StatusRequest {
    /** 目标状态（NORMAL=正常 / DISABLED=禁用 / LOCKED=锁定 / INACTIVE=未激活） */
    private UserStatus status;
}