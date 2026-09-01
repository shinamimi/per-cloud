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
        String sanitized = sanitizeFilename(filename);
        String extension = FileUtil.getExtension(sanitized);
        String baseName = sanitized.contains(".")
                ? sanitized.substring(0, sanitized.lastIndexOf("."))
                : sanitized;
        return "user/" + userId + "/" + simpleUUID() + "/" + baseName + "." + extension;
    }

    /** 个人文件对象路径（路径含 fileId，移动不影响对象） */
    public static String fileObject(Long userId, Long fileId, String fileName) {
        return "files/" + userId + "/" + fileId + "/" + sanitizeFilename(fileName);
    }

    /** 清理文件名中的路径穿越字符（..、/、\），防止路径注入 */
    public static String sanitizeFilename(String filename) {
        if (filename == null) return "";
        return filename.replaceAll("[\\\\/]", "_").replace("..", "_");
    }

    /** 分片临时对象路径 */
    public static String uploadChunkObject(Long userId, String uploadId, int seq) {
        return "uploads/" + userId + "/" + uploadId + "/chunk_" + seq;
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
