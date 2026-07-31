package com.cloud.backend.utils;

/**
 * ID 生成工具 —— UUID 和对象存储路径生成。
 *
 * 设计思路：
 * objectName 方法生成 MinIO 中的对象路径：user/{userId}/{uuid}/{原始文件名}
 * 使用 UUID 作为中间目录层，避免不同用户上传同名文件互相覆盖。
 * 文件模块使用 file-module.md 3.1 定义的前缀结构：
 * - uploads/{userId}/{uploadId}/chunk_{seq}      分片临时
 * - files/{userId}/{fileId}/{objectName}         个人文件
 * - thumbnails/{userId}/{fileId}.jpg             缩略图
 * - packages/{taskId}.zip                        打包
 */
public class IdUtil {

    public static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }

    public static String simpleUUID() {
        return uuid().replace("-", "");
    }

    /** 生成 MinIO 对象路径：user/{userId}/{uuid}/{filename} */
    public static String objectName(Long userId, String filename) {
        String extension = FileUtil.getExtension(filename);
        String baseName = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;
        return "user/" + userId + "/" + simpleUUID() + "/" + baseName + "." + extension;
    }

    /** 分片临时对象路径 */
    public static String uploadChunkObject(Long userId, String uploadId, int seq) {
        return "uploads/" + userId + "/" + uploadId + "/chunk_" + seq;
    }

    /** 个人文件对象路径（路径含 fileId，移动不影响对象） */
    public static String fileObject(Long userId, Long fileId, String fileName) {
        return "files/" + userId + "/" + fileId + "/" + fileName;
    }

    /** 缩略图对象路径 */
    public static String thumbnailObject(Long userId, Long fileId) {
        return "thumbnails/" + userId + "/" + fileId + ".jpg";
    }

    /** 打包下载产物对象路径 */
    public static String packageObject(String taskId) {
        return "packages/" + taskId + ".zip";
    }
}
