package com.cloud.backend.utils;

/**
 * ID 生成工具 —— UUID 和对象存储路径生成。
 *
 * 设计思路：
 * objectName 方法生成 MinIO 中的对象路径：user/{userId}/{uuid}/{原始文件名}
 * 使用 UUID 作为中间目录层，避免不同用户上传同名文件互相覆盖。
 * 同时也防止用户通过遍历 objectName 猜测其他人的文件。
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
}