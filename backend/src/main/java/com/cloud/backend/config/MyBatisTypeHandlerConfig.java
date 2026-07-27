package com.cloud.backend.config;

import com.cloud.backend.enums.*;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 枚举类型处理器注册中心。
 *
 * 设计思路：
 * 数据库中 Role、UserStatus、FileStatus、ShareStatus 字段存储的是 TINYINT 整数。
 * 但 Java 实体使用了枚举类型。MyBatis 默认的 EnumTypeHandler 会将枚举按 name() 存储（如 "ADMIN"），
 * 与数据库 TINYINT 不兼容。
 *
 * 解决方案：
 * 注册 EnumOrdinalTypeHandler 给这四个枚举，MyBatis 会使用枚举的 ordinal() 作为存储值。
 * 由于枚举声明顺序与 value 字段一一对应（如 Role.USER 在第一位，ordinal=0，value=0），
 * ordinal() 恰好等于自定义值，无需额外转换。
 *
 * OperationType 和 TargetType 在数据库中是 VARCHAR 类型，EnumTypeHandler（默认）即可，无需注册。
 */
@Configuration
public class MyBatisTypeHandlerConfig {

    public MyBatisTypeHandlerConfig(org.apache.ibatis.session.Configuration configuration) {
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        registry.register(Role.class, EnumOrdinalTypeHandler.class);
        registry.register(UserStatus.class, EnumOrdinalTypeHandler.class);
        registry.register(FileStatus.class, EnumOrdinalTypeHandler.class);
        registry.register(ShareStatus.class, EnumOrdinalTypeHandler.class);
    }
}