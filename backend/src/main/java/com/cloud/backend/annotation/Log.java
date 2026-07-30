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
    OperationType operation();
    TargetType target();
    String targetId() default "";
    String detail() default "";
}
