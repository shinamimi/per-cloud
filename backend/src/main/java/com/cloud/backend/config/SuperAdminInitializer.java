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
        newAdmin.setPassword(superAdminPassword);
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
