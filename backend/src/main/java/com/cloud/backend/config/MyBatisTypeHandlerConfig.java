package com.cloud.backend.config;

import com.cloud.backend.enums.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.stereotype.Component;

/**
 * MyBatis 枚举类型处理器注册 —— 统一配置各枚举与数据库 TINYINT / VARCHAR 的映射方式。
 *
 * 设计思路：
 * 1. 多数枚举（Role、UserStatus 等）按声明顺序用 EnumOrdinalTypeHandler 映射为 TINYINT，
 *    要求枚举声明顺序与 value 递增一致，业务判断用 getValue() 而非 ordinal()
 * 2. TeamMemberRole 存储自定义 value（0/10/20），与声明顺序不符，注册专用处理器
 * 3. 未在此注册的枚举（如好友请求状态）走 MyBatis 默认按枚举名称映射，无需处理
 *
 * 修改指引：
 * - 【习惯】修改 Role 映射方式        → registry.register(Role.class, ...)；改动后影响角色字段 DB 读写（TINYINT ↔ 枚举）
 * - 【习惯】修改 UserStatus 映射      → registry.register(UserStatus.class, ...)；改动后影响账号状态存取
 * - 【习惯】修改 FileStatus / FileType / ShareStatus / TeamStatus 映射 → 对应 registry.register(...) 一行
 * - 【习惯】新增枚举注册              → 新增 registry.register(Xxx.class, 对应处理器)；未注册则走 MyBatis 默认按名称映射
 * - 【习惯】注意：依赖 EnumOrdinalTypeHandler 的枚举按声明顺序映射为 TINYINT，新增枚举只能追加末尾，
 *         不能调整声明顺序，否则存量数据与枚举错位
 */
@Component
public class MyBatisTypeHandlerConfig implements ConfigurationCustomizer {

    /**
     * 注册各枚举的类型处理器到全局 TypeHandlerRegistry。
     * 注意：Role 等枚举依赖 EnumOrdinalTypeHandler 时，新增枚举只能追加在末尾，不能调整声明顺序。
     */
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
