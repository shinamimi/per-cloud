package com.cloud.backend.mapper;

import com.cloud.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户 Mapper —— MyBatis 动态代理接口。
 * findByAccount 方法同时匹配 username 和 email（SQL 中是 WHERE username=#{account} OR email=#{account}），
 * 用于登录时的账号输入兼容（支持用户名或邮箱登录）。
 */
@Mapper
public interface UserMapper {

    int insert(User user);

    User findById(Long id);

    User findByUsername(String username);

    User findByAccount(String account);

    User findByEmail(String email);

    List<User> findAll();

    int update(User user);

    int deleteById(Long id);

    /** 原子调整已用空间（上传扣减/删除释放），避免并发读写覆盖 */
    int updateUsedSpace(@Param("userId") Long userId, @Param("delta") long delta);

    /**
     * 按注册日期范围 + 角色/状态过滤查询用户（quota-batch 预览与执行共用）。
     *
     * @param role   ALL / USER（非 VIP 普通用户）/ VIP
     * @param status ALL / NORMAL / DISABLED / LOCKED / INACTIVE
     */
    List<User> findByQuotaFilter(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("role") String role,
                                 @Param("status") String status);

    /** 批量更新配额（quota-batch 执行） */
    int batchUpdateQuota(@Param("ids") List<Long> ids, @Param("quota") long quota);
}