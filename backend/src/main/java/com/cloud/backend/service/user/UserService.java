package com.cloud.backend.service.user;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.RoleEnum;
import com.cloud.backend.enums.UserStatusEnum;

import java.util.List;

public interface UserService {

    User register(User user);

    User findById(Long id);

    User findByUsername(String username);

    User findByAccount(String account);

    User findByEmail(String email);

    List<User> findAll();

    int update(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updatePassword(Long id, String rawPassword);

    User createAdmin(String username, String password, String email, String nickname, RoleEnum role);

    void updateUserStatus(Long id, UserStatusEnum status);

    void updateUserQuota(Long id, Long quota);

    void unlockUser(Long id);

    void deleteAdmin(Long id, Long currentUserId);

    void updateAdminRole(Long id, RoleEnum role);
}
