package com.cloud.backend.service.impl;

import com.cloud.backend.entity.Share;
import com.cloud.backend.mapper.ShareMapper;
import com.cloud.backend.service.ShareService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分享服务实现 —— 委托 ShareMapper。
 */
@Service
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;

    public ShareServiceImpl(ShareMapper shareMapper) {
        this.shareMapper = shareMapper;
    }

    @Override
    public Share create(Share share) {
        shareMapper.insert(share);
        return share;
    }

    @Override
    public Share findByToken(String shareToken) {
        return shareMapper.findByToken(shareToken);
    }

    @Override
    public Share findById(Long id) {
        return shareMapper.findById(id);
    }

    @Override
    public List<Share> listByUserId(Long userId) {
        return shareMapper.findByUserId(userId);
    }

    @Override
    public int update(Share share) {
        return shareMapper.update(share);
    }

    @Override
    public int removeById(Long id) {
        return shareMapper.deleteById(id);
    }

    @Override
    public List<Share> findAll() {
        return shareMapper.findAll();
    }
}