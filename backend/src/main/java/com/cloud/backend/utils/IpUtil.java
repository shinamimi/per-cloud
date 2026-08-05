package com.cloud.backend.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址获取工具。
 *
 * 设计思路：
 * 反向代理（Nginx）环境下 request.getRemoteAddr() 获取到的是代理的 IP，
 * 需要依次检查各种代理透传的请求头。
 * - X-Forwarded-For：标准代理头（可能有多个 IP，取第一个）
 * - X-Real-IP：Nginx 专有头
 * - Proxy-Client-IP / WL-Proxy-Client-IP：其他代理
 *
 * 修改指引：
 * - 【习惯】新增代理头来源          → getClientIp 中追加 request.getHeader(...) 判断；需与反向代理实际透传配置一致
 * - 【习惯】调整请求头优先级        → 调整 getClientIp 中 if 判断顺序；当前 X-Forwarded-For 优先
 * - 【习惯】修改 IP 取段规则        → 最后的逗号分割取第一段；需与代理透传格式一致，改动影响操作日志与防刷记录的 IP
 * - 【习惯】修改未知 IP 判定        → UNKNOWN 常量与各判断；改动影响无法解析时的回退链路（最终取 remoteAddr）
 */
public class IpUtil {

    private static final String UNKNOWN = "unknown";

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}