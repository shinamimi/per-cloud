package com.cloud.backend.config;

import com.cloud.backend.enums.*;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Configuration;

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