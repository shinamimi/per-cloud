package com.cloud.backend.mapper;

import com.cloud.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
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
}