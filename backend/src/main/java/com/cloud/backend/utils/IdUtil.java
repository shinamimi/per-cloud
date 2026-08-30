package com.cloud.backend.utils;

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
