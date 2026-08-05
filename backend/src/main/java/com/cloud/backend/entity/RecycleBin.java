package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 回收站实体 —— 对应数据库 t_recycle_bin 表。
 *
 * 设计思路：
 * 用户删除文件时不会立即从 MinIO 删除，而是将记录移入 recycle_bin 表，
 * 保留原文件信息和过期时间。在 expireTime 之前可以恢复。
 * 定时任务清理过期记录时同时删除 MinIO 中的原始对象（引用计数归零时）。
 * fileHash 用于物理清理时释放秒传引用计数。
 *
 * 修改指引：
 * - 【习惯】修改 id / userId        → Long id（t_recycle_bin.id 主键）/ Long userId（user_id）；仅定位回收站记录
 * - 【习惯】修改 fileId             → Long fileId；对应 t_recycle_bin.file_id，恢复时回写 t_file 的依据，改它需同步恢复逻辑
 * - 【习惯】修改 originalName       → String originalName；对应 t_recycle_bin.original_name，恢复/展示用原始文件名
 * - 【习惯】修改 objectName / fileHash → String objectName（t_recycle_bin.object_name，MinIO 对象路径，到期物理删除用）/
 *                            String fileHash（file_hash，物理清理时释放 t_file_hash 引用计数）；改任一项需联动物理删除逻辑
 * - 【习惯】修改 type               → Integer type；对应 t_recycle_bin.type（TINYINT），0-文件 1-目录，恢复时决定建文件还是建目录
 * - 【习惯】修改 teamId             → Long teamId；对应 t_recycle_bin.team_id，0-个人空间 >0-团队空间，恢复回原空间
 * - 【习惯】修改 deletedBy          → Integer deletedBy；对应 t_recycle_bin.deleted_by（TINYINT），0-用户自删(私有回收站)
 *                            1-管理员删(全局回收站)，决定回收站可见范围；改列名需同步索引 idx_deleted_by(deleted_by, team_id) 的 DDL
 * - 【习惯】修改 parentId           → Long parentId；对应 t_recycle_bin.parent_id，恢复时放回原目录
 * - 【习惯】修改 size / mimeType    → Long size（单位字节）/ String mimeType；恢复时还原 t_file 信息
 * - 【习惯】修改 deletedTime / expireTime → LocalDateTime deletedTime（t_recycle_bin.deleted_time 删除时间）/ expireTime（expire_time 过期时间）；
 *                            定时任务按 expire_time 清理（索引 idx_expire），改过期策略需联动清理任务
 */
@Data
public class RecycleBin {

    private Long id;
    private Long userId;
    private Long fileId;
    private String originalName;
    private String objectName;
    private String fileHash;
    private Integer type;
    private Long teamId;
    private Integer deletedBy;
    private Long parentId;
    private Long size;
    private String mimeType;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;
}
