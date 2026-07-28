package com.cloud.backend.config;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 超级管理员初始化器。
 *
 * 设计思路：
 * 1. 使用 ApplicationRunner（而非 @PostConstruct），确保 Spring Security 和数据库相关 Bean 已完全初始化
 * 2. 环境变量驱动：通过 SUPER_ADMIN_USERNAME / PASSWORD / EMAIL 三个环境变量配置管理员
 * 3. 首次启动自动创建超级管理员，后续启动跳过
 * 4. 如果环境变量密码变了，自动更新数据库密码（方便忘记密码时重置）
 *
 * 实现原理：
 * - findByUsername 查用户 → 不存在则创建
 * - 已存在且角色是 SUPER_ADMIN → 检查密码是否变化，变则更新
 * - 已存在但角色不是 SUPER_ADMIN → 跳过（防止占用普通用户账号）
 */
@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${super-admin.username}")
    private String superAdminUsername;

    @Value("${super-admin.password}")
    private String superAdminPassword;

    @Value("${super-admin.email}")
    private String superAdminEmail;

    public SuperAdminInitializer(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = userService.findByUsername(superAdminUsername);
        if (admin != null && admin.getRole() == Role.SUPER_ADMIN) {
            if (!passwordEncoder.matches(superAdminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(superAdminPassword));
                userService.update(admin);
                log.info("Super admin password updated: {}", superAdminUsername);
            }
            return;
        }
        if (admin != null && admin.getRole() != Role.SUPER_ADMIN) {
            log.warn("User {} exists but is not SUPER_ADMIN, skipping auto-init", superAdminUsername);
            return;
        }

        User newAdmin = new User();
        newAdmin.setUsername(superAdminUsername);
        newAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        newAdmin.setEmail(superAdminEmail);
        newAdmin.setNickname("Super Admin");
        newAdmin.setRole(Role.SUPER_ADMIN);
        newAdmin.setStatus(UserStatus.NORMAL);
        newAdmin.setQuota(FileConstants.DEFAULT_QUOTA);
        newAdmin.setUsedSpace(0L);
        userService.register(newAdmin);
        log.info("Super admin created: {}", superAdminUsername);
    }
}