package com.cloud.backend.config;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;
import com.cloud.backend.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /** 【统一】改后需同步 yml super-admin.username+读取方(SuperAdminInitializer run())（无单位，用户名字符串） */
    @Value("${super-admin.username}")
    private String superAdminUsername;

    /** 【统一】改后需同步 yml super-admin.password+读取方(SuperAdminInitializer run())（无单位，明文密码） */
    @Value("${super-admin.password}")
    private String superAdminPassword;

    /** 【统一】改后需同步 yml super-admin.email+读取方(SuperAdminInitializer run())（无单位，邮箱地址） */
    @Value("${super-admin.email}")
    private String superAdminEmail;

    public SuperAdminInitializer(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 应用启动回调：检查并初始化超级管理员账号。
     * 副作用：可能创建新用户或重置已有超管账号密码，均记录 INFO/WARN 日志。
     */
    @Override
    public void run(ApplicationArguments args) {
        User admin = userService.findByUsername(superAdminUsername);
        if (admin != null && admin.getRole() == Role.SUPER_ADMIN) {
            // 配置中的密码与库中不一致时补齐（BCrypt matches 判断），保证部署配置变更后仍可登录
            if (!passwordEncoder.matches(superAdminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(superAdminPassword));
                userService.update(admin);
                log.info("Super admin password updated: {}", superAdminUsername);
            }
            return;
        }
        if (admin != null && admin.getRole() != Role.SUPER_ADMIN) {
            // 用户名被业务账号占用：跳过自动初始化，避免覆盖用户数据
            log.warn("User {} exists but is not SUPER_ADMIN, skipping auto-init", superAdminUsername);
            return;
        }

        User newAdmin = new User();
        newAdmin.setUsername(superAdminUsername);
        newAdmin.setPassword(superAdminPassword);
        newAdmin.setEmail(superAdminEmail);
        newAdmin.setNickname("Super Admin");
        newAdmin.setRole(Role.SUPER_ADMIN);
        newAdmin.setStatus(UserStatus.NORMAL);
        newAdmin.setIsVip(false);
        newAdmin.setAdminBonusQuota(0L);
        newAdmin.setRewardQuota(0L);
        newAdmin.setQuota(FileConstants.DEFAULT_QUOTA);
        newAdmin.setUsedSpace(0L);
        userService.register(newAdmin);
        log.info("Super admin created: {}", superAdminUsername);
    }
}
