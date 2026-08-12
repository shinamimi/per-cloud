package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 上传策略响应 —— 前端在发起上传前拉取，用于：
 * - 单文件大小上限（VIP 差异化）：超限文件直接拒绝、不入传输队列
 * - 上传并发任务数上限（VIP 差异化）：超出部分在队列中排队等待
 *
 * 修改指引：
 * - 【统一】修改 maxSize         → long maxSize；单文件大小上限，单位：字节（非 KB/MB），0 表示不限制；前端按此拦截超限文件；改名需同步前端超限拦截逻辑与管理员配置
 * - 【统一】修改 maxConcurrent   → int maxConcurrent；上传并发任务数上限，0 表示不限制；超限任务前端排队等待；改名需同步前端排队逻辑与管理员配置
 * - 【统一】改单位/改语义        → 前端超限拦截与排队逻辑随字段语义变化，注意 maxSize 的单位换算陷阱；改后需同步前端拦截/排队逻辑与单位换算
 */
@Data
public class UploadPolicyResponse {

    /** 单文件大小上限（字节），0 表示不限制 */
    private long maxSize;

    /** 上传并发任务数上限，0 表示不限制 */
    private int maxConcurrent;
}
