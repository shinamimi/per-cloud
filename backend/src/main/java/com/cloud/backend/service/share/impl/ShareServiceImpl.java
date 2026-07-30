package com.cloud.backend.service.share.impl;

import com.cloud.backend.annotation.Log;
import com.cloud.backend.entity.Share;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.OperationType;
import com.cloud.backend.enums.ShareStatus;
import com.cloud.backend.enums.TargetType;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.ShareMapper;
import com.cloud.backend.service.share.ShareService;
import com.cloud.backend.service.system.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;
    private final OperationLogService operationLogService;

    public ShareServiceImpl(ShareMapper shareMapper, OperationLogService operationLogService) {
        this.shareMapper = shareMapper;
        this.operationLogService = operationLogService;
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

    @Override
    @Log(operation = OperationType.CANCEL_SHARE, target = TargetType.SHARE,
         targetId = "#id", detail = "'管理员取消分享'")
    public void adminCancelShare(Long id) {
        Share share = shareMapper.findById(id);
        if (share == null) {
            throw new BusinessException(ErrorCode.SHARE_NOT_FOUND);
        }
        share.setStatus(ShareStatus.CANCELED);
        shareMapper.update(share);
    }
}
