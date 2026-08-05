package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 提取码验证请求 —— POST /api/shares/access/{token}/verify。
 *
 * 修改指引：
 * - 【习惯】修改 password        → String password；提取码，请求体字段名对应验证接口入参；
 *                         服务端错误限次 5 次（Redis 计数，超限锁定，TTL 到期自动解锁），
 *                         验证通过打 ok 标记（24h 有效），前端验证成功前不可访问文件树/下载
 */
@Data
public class ShareVerifyRequest {

    private String password;
}
