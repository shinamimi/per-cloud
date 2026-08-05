package com.cloud.backend.mapper;

import com.cloud.backend.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分享 Mapper —— t_share 表。
 * 分享链接用 share_token（UUID）访问，支持提取码、过期时间、下载次数上限与目录快照。
 *
 * 设计思路：
 * 1. share_token 唯一标识分享，防止链接被猜解，命中唯一索引 uk_token(share_token)
 * 2. 下载次数用原子 SQL 递增（incrementDownloadCountIfAllowed），WHERE 内联过期/上限条件防并发超限
 * 3. 目录分享时由 ShareFileMapper 存快照，本表仅存主信息
 *
 * 修改指引：
 * - 【习惯】创建分享             → insert（XML：src/main/resources/mapper/ShareMapper.xml）；share_token 命中唯一索引
 *                          uk_token(share_token)，改 token 生成规则需同步 ShareService 与数据库 DDL
 * - 【习惯】查询分享             → findByToken / findById / findByUserId / findAll（XML 同上）；findByToken 为访问入口，
 *                          改状态/过期判断需同步 ShareService
 * - 【习惯】更新分享             → update / updateAllowDownload（XML 同上）；update 只改 status/expire_time/download_count，
 *                          改下载策略（allow_download/allow_save）需同步前端预览/下载交互
 * - 【习惯】下载计数             → incrementDownloadCountIfAllowed（XML 同上）；原子 UPDATE：status=0 且未过期且未达
 *                          max_download 才 +1，影响 0 行表示不允许下载（防并发超限），改上限逻辑需同时改 SQL 与 max_download 语义
 * - 【习惯】同文件活跃分享数      → countActiveByFileId（XML 同上）；统计 NORMAL/未过期分享数，用于 share.max-count-per-file 限制，
 *                          改限制规则需同步 ShareService 配置
 * - 【习惯】删除分享             → deleteById / deleteByFileId（XML 同上）；deleteByFileId 级联删除某文件全部分享，
 *                          需同步删除 ShareFileMapper 快照，否则遗留孤儿快照
 */
@Mapper
public interface ShareMapper {

    int insert(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> findByUserId(Long userId);

    int update(Share share);

    /** 更新下载开关（allow_download） */
    int updateAllowDownload(@Param("id") Long id, @Param("allowDownload") int allowDownload);

    /** 原子下载计数：仅 NORMAL + 未过期 + 未达上限时 +1，否则影响 0 行（防并发超限） */
    int incrementDownloadCountIfAllowed(Long id);

    /** 同文件当前活跃分享数（NORMAL/未过期），用于 share.max-count-per-file 限制 */
    int countActiveByFileId(Long fileId);

    int deleteById(Long id);

    int deleteByFileId(Long fileId);

    List<Share> findAll();
}
