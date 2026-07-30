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

    @Around("@annotation(com.cloud.backend.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);

        Long userId = AuthorizationPolicy.getCurrentUserId();
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

    @SuppressWarnings("unchecked")
    private <T> T evaluateSpel(String expression, ProceedingJoinPoint joinPoint, Object result, Class<T> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("result", result);

        return (T) parser.parseExpression(expression).getValue(context, type);
    }
}
