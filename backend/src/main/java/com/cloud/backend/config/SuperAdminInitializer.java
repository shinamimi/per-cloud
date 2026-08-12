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

/**
 * 超级管理员初始化 —— 应用启动时确保配置文件中指定的超级管理员账号存在且可用。
 *
 * 设计思路：
 * 1. 幂等初始化：账号已存在且角色正确时，仅当密码与配置不一致才重置密码
 * 2. 同名但非超管账号跳过初始化并告警，避免误改已注册的业务账号
 * 3. 新账号按注册流程创建（密码加密入库），配额取默认值（10GB）
 *
 * 修改指引：
 * - 【习惯】修改默认超管账号（用户名/密码/邮箱）→ application.yml 中 super-admin.username/password/email；
 *                                       改动后影响首次启动创建的账号与密码补齐行为
 * - 【习惯】修改密码补齐逻辑           → run() 中 passwordEncoder.matches() 分支；改动后影响配置变更后能否重新登录
 * - 【习惯】修改同名非超管账号的处理    → run() 中 "exists but is not SUPER_ADMIN" 分支；改动后影响是否覆盖业务账号
 * - 【习惯】修改新账号初始字段/配额     → run() 中 newAdmin 的 setter（如 setQuota(FileConstants.DEFAULT_QUOTA)）；
 *                               改动后影响新建超管的默认配额、会员状态等
 */
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
