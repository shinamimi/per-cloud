package com.cloud.backend.service;

import com.cloud.backend.entity.Share;

import java.util.List;

/**
 * 分享服务接口。
 * 对应文件分享、取消分享、访问分享等功能。
 */
public interface ShareService {

    Share create(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> listByUserId(Long userId);

    int update(Share share);

    int removeById(Long id);

    List<Share> findAll();
}