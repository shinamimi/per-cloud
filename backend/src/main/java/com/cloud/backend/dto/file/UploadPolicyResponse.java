package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 上传策略响应 —— 前端在发起上传前拉取，用于：
 * - 单文件大小上限（VIP 差异化）：超限文件直接拒绝、不入传输队列
 * - 上传并发任务数上限（VIP 差异化）：超出部分在队列中排队等待
 *
 * 修改指引：
 * - 【习惯】修改 maxSize         → long maxSize；单文件大小上限，单位：字节（非 KB/MB），0 表示不限制；前端按此拦截超限文件
 * - 【习惯】修改 maxConcurrent   → int maxConcurrent；上传并发任务数上限，0 表示不限制；超限任务前端排队等待
 * - 【习惯】改单位/改语义        → 前端超限拦截与排队逻辑随字段语义变化，注意 maxSize 的单位换算陷阱
 */
@Data
public class UploadPolicyResponse {

    /** 单文件大小上限（字节），0 表示不限制 */
    private long maxSize;

    /** 上传并发任务数上限，0 表示不限制 */
    private int maxConcurrent;
}
