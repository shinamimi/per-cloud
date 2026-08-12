package com.cloud.backend.entity;

import lombok.Data;

/**
 * 分享快照实体 —— 对应数据库 t_share_file 表。
 *
 * 目录分享时锁定创建时刻的文件树：
 * 分享后原文件的新增/改名/删除均不影响访客看到的内容；
 * 树结构由 parentId 指向本表 id 自包含（0=根节点）。
 * 下载/预览/转存时通过 fileId 回查原文件状态（删除/禁用则拒绝）。
 *
 * 修改指引：
 * - 【统一】修改 id / shareId      → Long id（t_share_file.id 主键）/ Long shareId（share_id，归属分享，索引 idx_share）；
 *                            改 shareId 需同步快照归属；
 *                            改后需同步 DB 列、idx_share 索引 DDL 与快照归属查询逻辑
 * - 【统一】修改 fileId            → Long fileId；对应 t_share_file.file_id，原文件 id，下载/预览/转存时回查 t_file 状态
 *                            （删除/禁用则拒绝），改它需同步回查逻辑；
 *                            改后需同步 DB 列与下载/预览/转存回查 t_file 的逻辑
 * - 【统一】修改 parentId          → Long parentId；对应 t_share_file.parent_id，快照树父节点（0=根，指向本表 id），
 *                            快照树自包含，改它影响快照树遍历；
 *                            改后需同步快照树遍历逻辑与 0=根 的取值语义
 * - 【习惯】修改 name              → String name；对应 t_share_file.name，快照中的文件名，改原文件不影响快照
 * - 【统一】修改 isDir             → Integer isDir；对应 t_share_file.is_dir（TINYINT），1=目录 0=文件，决定快照树节点类型；
 *                            改后需同步 DB 存量数据与快照树节点类型判断逻辑
 * - 【统一】修改 size / mimeType / extension / fileHash → Long size（单位字节）/ String mimeType / String extension /
 *                            String fileHash；快照冗余信息，供列表展示与秒传/下载校验；
 *                            改后需同步快照生成时复制（与 t_file 冗余信息一致）与秒传/下载校验
 */
@Data
public class ShareFile {

    private Long id;
    private Long shareId;
    /** 原文件 id */
    private Long fileId;
    /** 快照树父节点 id（0=根） */
    private Long parentId;
    private String name;
    /** 1=目录 0=文件 */
    private Integer isDir;
    private Long size;
    private String mimeType;
    private String extension;
    private String fileHash;
}
