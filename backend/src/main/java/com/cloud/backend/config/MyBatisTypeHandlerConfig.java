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
        registry.register(Role.class, EnumOrdinalTypeHandler.class);
        registry.register(UserStatus.class, EnumOrdinalTypeHandler.class);
        registry.register(FileStatus.class, EnumOrdinalTypeHandler.class);
        registry.register(FileType.class, EnumOrdinalTypeHandler.class);
        registry.register(ShareStatus.class, EnumOrdinalTypeHandler.class);
        registry.register(TeamStatus.class, EnumOrdinalTypeHandler.class);
        // 团队角色存 value（0/10/20）而非 ordinal，使用自定义 handler
        registry.register(TeamMemberRole.class, TeamMemberRoleTypeHandler.class);
        // 好友请求状态存 name（PENDING/ACCEPTED/REJECTED），默认 EnumTypeHandler 即可
    }
}
