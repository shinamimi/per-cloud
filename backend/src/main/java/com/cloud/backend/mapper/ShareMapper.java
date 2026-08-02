package com.cloud.backend.mapper;

import com.cloud.backend.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareMapper {

    int insert(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> findByUserId(Long userId);

    int update(Share share);

    /** 原子下载计数：仅 NORMAL + 未过期 + 未达上限时 +1，否则影响 0 行（防并发超限） */
    int incrementDownloadCountIfAllowed(Long id);

    /** 同文件当前活跃分享数（NORMAL/未过期），用于 share.max-count-per-file 限制 */
    int countActiveByFileId(Long fileId);

    int deleteById(Long id);

    int deleteByFileId(Long fileId);

    List<Share> findAll();
}
