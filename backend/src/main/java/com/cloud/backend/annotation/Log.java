package com.cloud.backend.annotation;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志标记注解 —— 标注在需要记录操作日志的方法上（登录、上传、删除等关键操作）。
 *
 * 设计思路：
 * 1. 由 LogAspect 切面统一拦截处理，业务代码无需注入日志服务
 * 2. targetId / detail 支持 SpEL 表达式（#参数名 / #result.字段），运行时解析
 * 3. 与 @Target(METHOD) + @Retention(RUNTIME) 配合，供切面在运行时读取
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /** 操作类型（LOGIN / UPLOAD_FILE / DELETE_FILE 等） */
    OperationType operation();
    /** 操作目标类型（USER / FILE / SHARE / TEAM） */
    TargetType target();
    /** 操作目标 ID 的 SpEL 表达式，默认空表示不记录目标 ID */
    String targetId() default "";
    /** 操作详情文本或 SpEL 表达式，默认空表示不记录详情 */
    String detail() default "";
}
