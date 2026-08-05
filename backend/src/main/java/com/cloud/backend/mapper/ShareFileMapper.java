package com.cloud.backend.mapper;

import com.cloud.backend.entity.ShareFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分享快照 Mapper —— t_share_file 表。
 * 分享创建时锁定目录树快照：文件被改名/删除/新增均不影响已分享内容。
 *
 * 设计思路：
 * 1. 快照独立成表，分享内容与实时文件解耦，下载/预览时按 file_id 校验原文件状态
 * 2. parent_id 指向快照表自身 id 形成快照树（0=根），与 t_file 的父节点无关
 * 3. findByIds 以 share_id + id 联合限定，防越权取他人分享节点
 *
 * 修改指引：
 * - 【习惯】写入快照             → insert / batchInsert（XML：src/main/resources/mapper/ShareFileMapper.xml）；
 *                          batchInsert 为 foreach 批量插入快照节点，改字段需同步 XML 与实体，快照与 t_file 解耦
 * - 【习惯】查询快照             → findByShareId / findByIds（XML 同上）；findByIds 以 share_id 与 id 联合限定，
 *                          改节点校验逻辑需同步 ShareService（防越权）；share_id 有索引 idx_share(share_id)，改字段名需同步 DDL
 * - 【习惯】删除分享时清快照       → deleteByShareId（XML 同上）；取消/删除分享时由 Service 调用，
 *                          删除顺序需与 t_share 主记录一致，否则遗留孤儿快照
 */
@Mapper
public interface ShareFileMapper {

    int insert(ShareFile shareFile);

    /** 批量插入快照节点 */
    int batchInsert(@Param("shareId") Long shareId, @Param("nodes") List<ShareFile> nodes);

    List<ShareFile> findByShareId(Long shareId);

    List<ShareFile> findByIds(@Param("shareId") Long shareId, @Param("ids") List<Long> ids);

    int deleteByShareId(Long shareId);
}
