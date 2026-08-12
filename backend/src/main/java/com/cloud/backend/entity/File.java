package com.cloud.backend.entity;

import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.enums.FileType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件/目录实体 —— 对应数据库 t_file 表。
 *
 * 设计思路：
 * 文件系统采用"目录树"模型（类似 Linux 文件系统）：
 * - 每个文件/目录有 parentId 指向父目录，根目录 parentId=0
 * - type 区分 FILE / DIRECTORY（统一表模型，is_directory 为兼容旧字段保留）
 * - category 为文件分类（0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他），搜索按类型过滤
 * - teamId 归属：0 表示个人空间，>0 表示团队空间
 * - objectName 是 MinIO 中的对象路径（仅文件有此值）
 * - fileHash 用于秒传（相同文件哈希直接引用已有 object，不上传）
 *
 * 修改指引：
 * - 【统一】修改 id / userId / teamId / parentId → Long id（t_file.id 主键）/ Long userId（user_id）/ Long teamId（team_id，
 *                            0=个人空间 >0=团队空间）/ Long parentId（parent_id，0=根目录）；改列名需同步 DDL；
 *                            改后需同步 DB 列与 Service 组装（teamId/parentId 取值为 0 的语义判断处）
 * - 【统一】修改 name / path     → String name（t_file.name）/ String path（t_file.path 完整路径）；唯一索引
 *                            uk_user_parent_name(user_id,parent_id,name,team_id) 约束同空间同目录下 name 唯一，
 *                            改字段名或放宽唯一性需同步 DDL（业务层重名自动加后缀）；
 *                            改后需同步 DDL 唯一索引 uk_user_parent_name 与业务层重名自动加后缀逻辑
 * - 【统一】修改 size            → Long size；对应 t_file.size，单位字节，影响配额 used_space 计算与展示；
 *                            改后需同步配额 used_space 计算与展示口径（与 t_file.size 单位一致）
 * - 【习惯】修改 mimeType / extension → String mimeType（t_file.mime_type）/ String extension（t_file.extension）；仅展示与下载判断
 * - 【统一】修改 fileHash        → String fileHash；对应 t_file.file_hash（SHA256），秒传索引（t_file_hash）与对象禁用
 *                            （t_disabled_object）判定的依据；
 *                            改后需同步 t_file_hash 秒传索引、t_disabled_object 判定逻辑与 DB 列
 * - 【统一】修改 isDirectory / type → Integer isDirectory（t_file.is_directory，兼容旧字段）/ FileType type（t_file.type TINYINT，
 *                            FILE=0/DIRECTORY=1，见 enums/FileType.java，按 ordinal 存库）；isDir() 兼容两者，改枚举见 FileType 修改指引；
 *                            改后需同步 DB 存量数据、TypeHandler 存储与 isDir() 兼容判断
 * - 【统一】修改 category        → Integer category；对应 t_file.category（TINYINT），0-图片 1-文档 2-视频 3-音频 4-压缩包 5-其他，
 *                            搜索按类型过滤；
 *                            改后需同步 DB 存量数据与搜索按类型过滤逻辑
 * - 【统一】修改 objectName      → String objectName；对应 t_file.object_name，MinIO 对象路径（仅文件有值）；
 *                            改它会导致下载/删除定位不到物理对象；
 *                            改后需同步 MinIO 物理对象、DB 存量数据与下载/删除定位逻辑
 * - 【统一】修改 status          → FileStatus status；对应 t_file.status（TINYINT），DELETED=0/NORMAL=1/DISABLED=2
 *                            （见 enums/FileStatus.java，按 ordinal 存库）；逻辑删除（FileServiceImpl 置 DELETED + 写回收站）
 *                            与管理员禁用均在此，改枚举见 FileStatus 修改指引；
 *                            改后需同步 DB 存量数据、FileServiceImpl 逻辑删除与回收站写入/过滤条件
 * - 【习惯】修改 createdAt / updatedAt → LocalDateTime；自动维护，无业务联动
 */
@Data
public class File {

    private Long id;
    private Long userId;
    private Long teamId;
    private Long parentId;
    private String name;
    private String path;
    private Long size;
    private String mimeType;
    private String extension;
    private String fileHash;
    private Integer isDirectory;
    private FileType type;
    private Integer category;
    private String objectName;
    private FileStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 兼容判断：是否为目录（type == DIRECTORY 或旧数据 is_directory == 1） */
    public boolean isDir() {
        return type == FileType.DIRECTORY || (type == null && isDirectory != null && isDirectory == 1);
    }
}
