package com.cloud.backend.service.user;

import com.cloud.backend.dto.admin.RoleChangeRequest;
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

    void resetUserPassword(Long userId, String newPassword);

    long calculateTotalQuota(User user);

    /** 用户剩余可用空间 = 总配额 - 已用（用户不存在抛 USER_NOT_FOUND） */
    long getRemainingQuota(Long userId);

    /** 原子调整已用空间（上传扣减为正、删除释放为负） */
    void changeUsedSpace(Long userId, long delta);

    List<User> listCandidates();

    void batchUpdateAdminRole(List<RoleChangeRequest> changes);

    /** 用户搜索（好友/团队拉人）：用户名/邮箱前缀模糊，最多 20 条 */
    List<User> searchUsers(String keyword);
}
