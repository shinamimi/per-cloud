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
 *
 * 修改指引：
 * - 【习惯】修改注解名称            → public @interface Log；项目自定义注解名，改名需同步全部使用处与切面
 * - 【固定】修改可标注元素（如方法）  → @Target(ElementType.METHOD)；@Target 与 ElementType 为 Java 固定名，
 *                              可选元素见 CODING_STANDARDS §1.6.8 的 @Target 速查表（类/字段/参数/构造器等）
 * - 【固定】修改运行时是否可读取     → @Retention(RetentionPolicy.RUNTIME)；@Retention 与 RetentionPolicy 为 Java 固定名，
 *                              需切面运行时读取必须为 RUNTIME，可选值见 CODING_STANDARDS §1.6.8（SOURCE/CLASS/RUNTIME）
 * - 【习惯】修改 operation 字段名   → OperationType operation()；项目自定义字段名，字段类型为自定义枚举 OperationType，
 *                              取值 LOGIN / UPLOAD_FILE / DELETE_FILE 等，定义于 enums/OperationType.java
 * - 【习惯】修改 target 字段名      → TargetType target()；项目自定义字段名，字段类型为自定义枚举 TargetType，
 *                              取值 USER / FILE / SHARE / TEAM，定义于 enums/TargetType.java
 * - 【习惯】修改 targetId 字段名    → String targetId()；项目自定义字段名；SpEL 表达式，默认空表示不记录目标 ID
 * - 【习惯】修改 detail 字段名      → String detail()；项目自定义字段名；SpEL 表达式，默认空表示不记录详情
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /** 操作类型，自定义枚举 OperationType（enums/OperationType.java）：LOGIN/REGISTER/UPLOAD_FILE/DOWNLOAD_FILE/DELETE_FILE/RESTORE_FILE/CREATE_DIRECTORY/CREATE_SHARE/CANCEL_SHARE/DELETE_SHARE/UPDATE_USER/DISABLE_FILE/ENABLE_FILE/TEAM_CREATE/TEAM_DISSOLVE/TEAM_INVITE/TEAM_REMOVE/TEAM_LEAVE/RESET_PASSWORD */
    OperationType operation();
    /** 操作目标类型，自定义枚举 TargetType（enums/TargetType.java）：USER/FILE/SHARE/TEAM */
    TargetType target();
    /** 操作目标 ID 的 SpEL 表达式，默认空表示不记录目标 ID */
    String targetId() default "";
    /** 操作详情文本或 SpEL 表达式，默认空表示不记录详情 */
    String detail() default "";
}
