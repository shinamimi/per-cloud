package com.cloud.backend.utils;

import com.cloud.backend.constant.FileConstants;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.FileMapper;

import java.text.DecimalFormat;
import java.util.Set;

public class FileUtil {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "tar", "gz",
            "mp3", "mp4", "avi", "mov", "mkv",
            "json", "xml", "csv"
    );

    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml", "md");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "avi", "mov", "mkv");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "flac", "wav", "m4a", "aac", "ogg");
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip", "rar", "7z", "tar", "gz", "bz2");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "json", "xml", "csv", "log", "yml", "yaml", "java", "sql");

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
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
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

    public static boolean isText(String extension) {
        return extension != null && TEXT_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isAllowed(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /** 文件分类（与 FileConstants 分类常量一致），用于搜索类型过滤 */
    public static int categoryOf(String extension) {
        if (extension == null || extension.isEmpty()) return FileConstants.OTHER;
        String ext = extension.toLowerCase();
        if (IMAGE_EXTENSIONS.contains(ext)) return FileConstants.IMAGE;
        if (DOCUMENT_EXTENSIONS.contains(ext)) return FileConstants.DOCUMENT;
        if (VIDEO_EXTENSIONS.contains(ext)) return FileConstants.VIDEO;
        if (AUDIO_EXTENSIONS.contains(ext)) return FileConstants.AUDIO;
        if (ARCHIVE_EXTENSIONS.contains(ext)) return FileConstants.ARCHIVE;
        return FileConstants.OTHER;
    }

    /** 将字节数转为可读的大小字符串（如 "1.5 MB"） */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        double size = bytes / Math.pow(1024, unitIndex);
        return new DecimalFormat("#,##0.#").format(size) + " " + units[unitIndex];
    }

    /**
     * 在 (userId, parentId) 下为 baseName 生成唯一名：存在同名（仅活跃记录）时追加全角后缀「（2）」「（3）」…
     * 删除/恢复流程共用，保证与唯一索引 uk_user_parent_name 一致。
     */
    public static String resolveUniqueName(FileMapper fileMapper, Long userId, Long parentId, String baseName) {
        String name = baseName;
        int suffix = 2;
        while (fileMapper.findByUserIdAndParentIdAndName(userId, parentId, name) != null) {
            String stem = stripSuffix(baseName);
            name = stem + "（" + suffix + "）" + getExtensionPart(baseName);
            suffix++;
            if (suffix > 1000) {
                throw new BusinessException(ErrorCode.FILE_NAME_DUPLICATE);
            }
        }
        return name;
    }

    private static String stripSuffix(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }

    private static String getExtensionPart(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(dot);
        }
        return "";
    }
}
