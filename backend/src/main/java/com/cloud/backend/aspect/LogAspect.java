package com.cloud.backend.aspect;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.authorization.AuthorizationPolicy;
import com.cloud.backend.entity.OperationLog;
import com.cloud.backend.service.system.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class LogAspect {

    private final OperationLogService operationLogService;
    private final ExpressionParser parser;

    public LogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
        this.parser = new SpelExpressionParser();
    }

    /**
     * 环绕通知：执行目标方法后组装 OperationLog 并落库。
     * 若当前线程无登录用户则跳过记录（不影响业务结果）。
     */
    @Around("@annotation(com.cloud.backend.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        Long userId = AuthorizationPolicy.getCurrentUserId();
        // 未登录请求（无安全上下文）不记录日志
        if (userId == null) {
            return result;
        }

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperation(logAnnotation.operation());
        log.setTargetType(logAnnotation.target());

        String targetIdExpr = logAnnotation.targetId();
        if (!targetIdExpr.isEmpty()) {
            Long targetId = evaluateSpel(targetIdExpr, joinPoint, result, Long.class);
            log.setTargetId(targetId);
        }

        String detailExpr = logAnnotation.detail();
        if (!detailExpr.isEmpty()) {
            String detail = evaluateSpel(detailExpr, joinPoint, result, String.class);
            log.setDetail(detail);
        }

        operationLogService.log(log);
        return result;
    }

    /**
     * 解析 SpEL 表达式：参数名映射为上下文变量，另固定暴露 result（方法返回值）。
     * 返回类型由调用方指定（Long / String），解析结果按该类型强制转换。
     */
    @SuppressWarnings("unchecked")
    private <T> T evaluateSpel(String expression, ProceedingJoinPoint joinPoint, Object result, Class<T> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        // 依赖编译期 -parameters 选项提供的参数名（否则为 arg0/arg1）
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("result", result);

        return (T) parser.parseExpression(expression).getValue(context, type);
    }
}
