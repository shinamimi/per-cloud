package com.cloud.backend.service;

import com.cloud.backend.entity.Share;

import java.util.List;

public interface ShareService {

    Share create(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> listByUserId(Long userId);

    int update(Share share);

    int removeById(Long id);

    List<Share> findAll();
}