package com.cloud.backend.mapper;

import com.cloud.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int insert(User user);

    User findById(Long id);

    User findByUsername(String username);

    User findByEmail(String email);

    int update(User user);

    int deleteById(Long id);
}