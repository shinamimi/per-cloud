package com.cloud.backend.dto.file;

import lombok.Data;

@Data
public class UploadPolicyResponse {

    /** 单文件大小上限（字节），0 表示不限制 */
    private long maxSize;

    /** 上传并发任务数上限，0 表示不限制 */
    private int maxConcurrent;
}
