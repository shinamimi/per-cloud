package com.cloud.backend.service;

import com.cloud.backend.entity.User;
import java.util.List;

public interface UserService {

    User register(User user);

    User login(String username, String password);

    User findById(Long id);

    User findByUsername(String username);

    User findByAccount(String account);

    User findByEmail(String email);

    List<User> findAll();

    int update(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}