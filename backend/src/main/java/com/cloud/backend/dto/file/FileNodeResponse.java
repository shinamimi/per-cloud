package com.cloud.backend.dto.file;

import com.cloud.backend.entity.File;
import com.cloud.backend.enums.FileType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件列表节点响应。
 * userId/uploaderName 为上传者信息：个人文件列表用不到，团队文件列表由服务层填充。
 *
 * 修改指引：
 * - 【统一】修改 id / parentId / name → Long id / Long parentId / String name；对应 t_file 主键/父目录/文件名，前端列表定位用；改名需同步前端列表逻辑与 FileNodeResponse.from 组装
 * - 【习惯】修改 size            → Long size；文件大小，单位：字节，前端展示需自行换算 KB/MB/GB
 * - 【习惯】修改 mimeType / extension → String mimeType / String extension；仅展示与预览判断用
 * - 【统一】修改 isDirectory / type → Boolean isDirectory / FileType type（t_file.type TINYINT，FILE=0/DIRECTORY=1，
 *                         定义于 enums/FileType.java）；两者必须保持一致，改 type 需同步改 isDirectory，否则前端目录判断错乱；改后需同步 File 实体 type 映射与前端目录判断
 * - 【统一】修改 category        → Integer category；文件分类（FileConstants：IMAGE=0/DOCUMENT=1/VIDEO=2/AUDIO=3/OTHER=5），
 *                         前端用于分类筛选与文件图标；改取值需同步 FileConstants 与前端分类筛选/图标映射
 * - 【习惯】修改 userId / uploaderName → 上传者信息；个人文件列表为 null 勿依赖，团队文件列表由服务层填充 uploaderName
 * - 【习惯】修改 createdAt / updatedAt → LocalDateTime 创建/更新时间，前端直接展示或做排序
 */
@Data
public class FileNodeResponse {

    private Long id;
    private Long parentId;
    private String name;
    private Long size;
    private String mimeType;
    private String extension;
    private Boolean isDirectory;
    private FileType type;
    private Integer category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String uploaderName;

    public static FileNodeResponse from(File file) {
        FileNodeResponse response = new FileNodeResponse();
        response.setId(file.getId());
        response.setParentId(file.getParentId());
        response.setName(file.getName());
        response.setSize(file.getSize());
        response.setMimeType(file.getMimeType());
        response.setExtension(file.getExtension());
        response.setIsDirectory(file.isDir());
        response.setType(file.getType());
        response.setCategory(file.getCategory());
        response.setCreatedAt(file.getCreatedAt());
        response.setUpdatedAt(file.getUpdatedAt());
        response.setUserId(file.getUserId());
        return response;
    }
}
