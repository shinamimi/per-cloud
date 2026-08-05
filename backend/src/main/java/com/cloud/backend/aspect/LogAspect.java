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

/**
 * 操作日志切面 —— 拦截所有标注 @Log 的方法，在方法执行成功后记录操作日志。
 *
 * 设计思路：
 * 1. 先执行目标方法再记日志：业务失败（抛异常）时不会留下误导性日志
 * 2. 当前用户从安全上下文获取；未登录场景（如开放接口）直接跳过记录
 * 3. 目标 ID 与详情支持 SpEL 表达式，通过方法参数名绑定上下文，可引用返回值（#result）
 *
 * 修改指引：
 * - 【习惯】修改日志记录时机        → around 中 joinPoint.proceed() 之后记录；如需记录失败日志需在 catch 分支补充
 * - 【习惯】修改 SpEL 上下文绑定    → evaluateSpel 中的变量绑定（参数名 + result）；新增变量需同步 @Log 注解的可用表达式说明
 * - 【习惯】修改未登录跳过策略      → around 中 userId == null 判断；当前未登录不记录，改动影响日志覆盖率
 * - 【习惯】修改日志落库方式        → operationLogService.log(...)；改异步可减少主流程延迟，需注意失败兜底
 * - 【习惯】修改日志字段组装        → OperationLog 各 setter；需与 OperationLogService 的入库字段保持一致
 */
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
