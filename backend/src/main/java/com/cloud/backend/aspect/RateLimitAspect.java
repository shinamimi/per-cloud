package com.cloud.backend.aspect;

import com.cloud.backend.annotation.RateLimit;
import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * API 速率限制切面
 * 基于 Redis 滑动窗口实现
 */
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.window()));
        }
        if (count != null && count > rateLimit.limit()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "请求过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        StringBuilder sb = new StringBuilder("rate_limit:");
        sb.append(rateLimit.key()).append(":");

        switch (rateLimit.dimension()) {
            case IP:
                sb.append(getClientIp());
                break;
            case USER:
                try {
                    Long userId = AuthorizationPolicy.getCurrentUserId();
                    sb.append("user:").append(userId);
                } catch (Exception e) {
                    sb.append("user:anonymous:").append(getClientIp());
                }
                break;
            case GLOBAL:
                sb.append("global");
                break;
        }
        return sb.toString();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "unknown";
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
