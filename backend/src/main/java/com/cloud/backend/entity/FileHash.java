package com.cloud.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 秒传索引实体 —— 对应数据库 t_file_hash 表。
 *
 * 设计思路：
 * 全局 SHA256 索引表，hash 唯一。命中即秒传：新文件引用同一物理对象，ref_count +1。
 * 物理删除（回收站 30 天清理）时 ref_count -1，归零才真正删除 MinIO 对象。
 *
 * 修改指引：
 * - 【统一】修改 fileHash        → String fileHash；对应 t_file_hash.file_hash（SHA256），唯一索引 uk_hash 约束 hash 全局唯一，
 *                            改字段名/长度需同步迁移脚本 DDL；它是秒传去重的核心依据；
 *                            改后需同步 DB 列、uk_hash 唯一索引 DDL 与秒传去重逻辑
 * - 【统一】修改 objectName      → String objectName；对应 t_file_hash.object_name，共享物理对象在 MinIO 的路径；
 *                            改路径需同步存量数据与物理对象，否则引用它的文件下载失败；
 *                            改后需同步 DB 存量数据、MinIO 物理对象与引用它的文件下载逻辑
 * - 【统一】修改 size / mimeType → Long size（单位字节）/ String mimeType；仅秒传时复制给新文件记录，无独立业务逻辑；
 *                            改后需同步秒传复制到新文件记录的逻辑（与 t_file.size 单位一致）
 * - 【统一】修改 refCount        → Integer refCount；对应 t_file_hash.ref_count，全局引用计数，归零才删除 MinIO 对象；
 *                            增删文件均会增减，改逻辑需联动秒传引用与回收站清理；
 *                            改后需同步秒传引用增减逻辑、回收站清理与 MinIO 删除时序
 * - 【习惯】修改 id / createdAt / updatedAt → 主键与自动维护时间，无业务联动
 */
@Data
public class FileHash {

    private Long id;
    private String fileHash;
    private String objectName;
    private Long size;
    private String mimeType;
    private Integer refCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
