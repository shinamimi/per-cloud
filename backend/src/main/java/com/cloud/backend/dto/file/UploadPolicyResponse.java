package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 上传策略响应 —— 前端在发起上传前拉取，用于：
 * - 单文件大小上限（VIP 差异化）：超限文件直接拒绝、不入传输队列
 * - 上传并发任务数上限（VIP 差异化）：超出部分在队列中排队等待
 */
@Data
public class UploadPolicyResponse {

    /** 单文件大小上限（字节），0 表示不限制 */
    private long maxSize;

    /** 上传并发任务数上限，0 表示不限制 */
    private int maxConcurrent;
}
