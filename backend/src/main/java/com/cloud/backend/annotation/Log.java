package com.cloud.backend.annotation;

import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.TargetType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
