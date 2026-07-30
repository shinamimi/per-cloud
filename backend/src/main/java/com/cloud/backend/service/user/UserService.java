package com.cloud.backend.service.user;

import com.cloud.backend.entity.User;
import com.cloud.backend.enums.Role;
import com.cloud.backend.enums.UserStatus;

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

    User createAdmin(String username, String password, String email, String nickname, Role role);

    void updateUserStatus(Long id, UserStatus status);

    void updateUserQuota(Long id, Long quota);

    void unlockUser(Long id);

    void deleteAdmin(Long id, Long currentUserId);

    void updateAdminRole(Long id, Role role);
}
