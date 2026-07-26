package com.cloud.backend.utils;

import java.util.UUID;

public class IdUtil {

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String simpleUUID() {
        return uuid().replace("-", "");
    }

    public static String objectName(Long userId, String filename) {
        String extension = FileUtil.getExtension(filename);
        String baseName = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf("."))
                : filename;
        return "user/" + userId + "/" + simpleUUID() + "/" + baseName + "." + extension;
    }
}