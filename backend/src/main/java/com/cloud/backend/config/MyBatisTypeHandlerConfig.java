package com.cloud.backend.config;

import com.cloud.backend.enums.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.stereotype.Component;

@Component
public class MyBatisTypeHandlerConfig implements ConfigurationCustomizer {

    @Override
    public void customize(Configuration configuration) {
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        registry.register(RoleEnum.class, EnumOrdinalTypeHandler.class);
        registry.register(UserStatusEnum.class, EnumOrdinalTypeHandler.class);
        registry.register(FileStatusEnum.class, EnumOrdinalTypeHandler.class);
        registry.register(ShareStatusEnum.class, EnumOrdinalTypeHandler.class);
    }
}
