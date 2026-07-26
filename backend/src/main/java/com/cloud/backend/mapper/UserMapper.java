package com.cloud.backend.mapper;

import com.cloud.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

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