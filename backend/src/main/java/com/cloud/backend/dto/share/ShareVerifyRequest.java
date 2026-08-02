package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 提取码验证请求 —— POST /api/shares/access/{token}/verify。
 */
@Data
public class ShareVerifyRequest {

    private String password;
}
