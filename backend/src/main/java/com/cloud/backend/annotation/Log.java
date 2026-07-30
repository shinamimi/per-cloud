package com.cloud.backend.annotation;

import com.cloud.backend.enums.OperationTypeEnum;
import com.cloud.backend.enums.TargetTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    OperationTypeEnum operation();
    TargetTypeEnum target();
    String targetId() default "";
    String detail() default "";
}
