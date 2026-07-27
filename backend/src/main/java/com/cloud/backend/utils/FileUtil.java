package com.cloud.backend.utils;

import java.text.DecimalFormat;
import java.util.Set;

/**
 * 文件处理工具 —— 扩展名提取、MIME 类型映射、类型判断、大小格式化。
 *
 * 设计思路：
 * 集中管理文件类型相关的规则，避免散落在各个 Service 中。
 * ALLOWED_EXTENSIONS 用于上传时的白名单校验。
 * getMimeType 用于文件上传到 MinIO 时设置正确的 Content-Type。
 */
public class FileUtil {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz",
            "mp3", "mp4", "avi", "mov", "mkv",
            "json", "xml", "csv"
    );

    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /** 根据扩展名返回 MIME Type，未知类型返回 application/octet-stream */
    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) return "application/octet-stream";
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "7z" -> "application/x-7z-compressed";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    public static boolean isImage(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isAllowed(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /** 将字节数转为可读的大小字符串（如 "1.5 MB"） */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        double size = bytes / Math.pow(1024, unitIndex);
        return new DecimalFormat("#,##0.#").format(size) + " " + units[unitIndex];
    }
}