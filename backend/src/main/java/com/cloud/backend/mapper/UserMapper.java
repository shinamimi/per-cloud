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
 *
 * 修改指引：
 * - 【习惯】增删用户             → insert / deleteById（XML：src/main/resources/mapper/UserMapper.xml）；username/email 命中唯一索引
 *                          uk_username(username)、uk_email(email)，改字段名需同步数据库 DDL，否则重复注册唯一键冲突
 * - 【习惯】查询用户             → findById / findByIds / findByUsername / findByEmail / findByAccount / findAll（XML 同上）；
 *                          findByIds 为 IN 批量查询（团队文件列表填充上传者等场景），findByAccount 以 username OR email
 *                          兼容登录，改登录匹配规则需同步 AuthService
 * - 【习惯】更新用户             → update（XML 同上）；全字段更新并刷新 updated_at，改字段需同步 XML 与实体，注意全量覆盖语义
 * - 【习惯】调整已用空间         → updateUsedSpace（XML 同上）；SQL 为 used_space = GREATEST(used_space + #{delta}, 0) 原子更新、
 *                          负数钳制为 0，上传/删除时由 Service 调用，改扣减语义需同步 XML 与配额校验逻辑
 * - 【习惯】配额批量操作         → findByQuotaFilter / batchUpdateQuota（XML 同上）；findByQuotaFilter 为动态条件过滤
 *                          （日期范围 + role/status），与 quota-batch 预览/执行共用，改过滤规则需同步 XML 与入参语义；
 *                          batchUpdateQuota 为 IN 批量更新 quota，改配额来源（adminBonus/reward）需同步 Service 层
 * - 【习惯】用户搜索             → searchByKeyword（XML 同上）；username/email 前缀模糊匹配且 status=1，LIMIT #{limit}（最多 20 条），
 *                          改搜索范围或条数需同步 XML 与入参
 */
@Mapper
public interface UserMapper {

    int insert(User user);

    User findById(Long id);

    /** 批量查询（团队文件列表填充上传者等场景） */
    List<User> findByIds(@Param("ids") List<Long> ids);

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

    /** 用户搜索（好友/团队拉人）：用户名或邮箱前缀模糊匹配，最多 20 条 */
    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}