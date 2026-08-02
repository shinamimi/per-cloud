package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.FileProperties;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.entity.File;
import com.cloud.backend.enums.ErrorCode;
import com.cloud.backend.enums.FileStatus;
import com.cloud.backend.exception.BusinessException;
import com.cloud.backend.mapper.DisabledObjectMapper;
import com.cloud.backend.service.file.FileService;
import com.cloud.backend.service.file.PreviewService;
import com.cloud.backend.service.file.StorageService;
import com.cloud.backend.utils.FileUtil;
import com.cloud.backend.utils.IdUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 预览服务实现。
 *
 * 设计思路（file-module.md 第六节）：
 * - 图片：presigned URL 直链 + Thumbnailator 缩略图（thumbnails/ 前缀，首次生成后复用）
 * - 视频/音频/PDF：presigned URL，浏览器原生播放/阅读（Range 请求由 MinIO 支持）
 * - 文本：大小在限制内直接读内容返回；Office 等仅下载
 */
@Service
public class PreviewServiceImpl implements PreviewService {

    private static final Logger log = LoggerFactory.getLogger(PreviewServiceImpl.class);

    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_AUDIO = "AUDIO";
    private static final String TYPE_PDF = "PDF";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_UNSUPPORTED = "UNSUPPORTED";

    private final FileService fileService;
    private final StorageService storageService;
    private final FileProperties fileProperties;
    private final com.cloud.backend.service.admin.AdminSettingsService adminSettingsService;
    private final DisabledObjectMapper disabledObjectMapper;

    public PreviewServiceImpl(FileService fileService, StorageService storageService, FileProperties fileProperties,
                              com.cloud.backend.service.admin.AdminSettingsService adminSettingsService,
                              DisabledObjectMapper disabledObjectMapper) {
        this.fileService = fileService;
        this.storageService = storageService;
        this.fileProperties = fileProperties;
        this.adminSettingsService = adminSettingsService;
        this.disabledObjectMapper = disabledObjectMapper;
    }

    @Override
    public FilePreviewResponse preview(Long userId, Long fileId) {
        return previewFile(userId, fileService.getOwnedFile(userId, fileId));
    }

    @Override
    public FilePreviewResponse previewFile(Long userId, File file) {
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.PREVIEW_UNSUPPORTED);
        }
        // 禁用/对象级禁用文件不可预览（docs/admin-file-management.md：用户端不可下载/预览，管理员后台可预览）
        if (file.getStatus() == FileStatus.DISABLED) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        if (file.getFileHash() != null && !file.getFileHash().isEmpty()
                && disabledObjectMapper.countBlocked(file.getFileHash(), userId) > 0) {
            throw new BusinessException(ErrorCode.FILE_DISABLED);
        }
        return previewContent(userId, file);
    }

    @Override
    public FilePreviewResponse previewFileForAdmin(File file) {
        if (file.isDir() || file.getObjectName() == null || file.getObjectName().isEmpty()) {
            throw new BusinessException(ErrorCode.PREVIEW_UNSUPPORTED);
        }
        // 管理员后台预览不受禁用限制（docs/admin-file-management.md：用于决定解禁）
        return previewContent(file.getUserId(), file);
    }

    private FilePreviewResponse previewContent(Long userId, File file) {
        String extension = file.getExtension() == null ? "" : file.getExtension().toLowerCase();
        String url = storageService.generateDownloadUrl(file.getObjectName(), adminSettingsService.getDownloadLinkTtlMinutes());

        FilePreviewResponse response = new FilePreviewResponse();
        response.setName(file.getName());
        response.setSize(file.getSize());

        if (FileUtil.isImage(extension)) {
            response.setType(TYPE_IMAGE);
            response.setUrl(url);
            response.setThumbnailUrl(thumbnailUrl(userId, file, extension, url));
            return response;
        }
        if ("mp4".equals(extension) || "webm".equals(extension)) {
            response.setType(TYPE_VIDEO);
            response.setUrl(url);
            return response;
        }
        if ("mp3".equals(extension) || "flac".equals(extension) || "wav".equals(extension)
                || "m4a".equals(extension) || "aac".equals(extension) || "ogg".equals(extension)) {
            response.setType(TYPE_AUDIO);
            response.setUrl(url);
            return response;
        }
        if ("pdf".equals(extension)) {
            response.setType(TYPE_PDF);
            response.setUrl(url);
            return response;
        }
        if (FileUtil.isText(extension)) {
            long size = file.getSize() == null ? 0 : file.getSize();
            if (size > fileProperties.getPreviewTextMaxSize()) {
                response.setType(TYPE_UNSUPPORTED);
                return response;
            }
            try (InputStream input = storageService.download(file.getObjectName())) {
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                response.setType(TYPE_TEXT);
                response.setContent(content);
                return response;
            } catch (IOException e) {
                log.warn("Read text content failed: fileId={}", file.getId(), e);
                response.setType(TYPE_UNSUPPORTED);
                return response;
            }
        }
        response.setType(TYPE_UNSUPPORTED);
        return response;
    }

    /** 生成/复用缩略图（gif/svg 不生成，直接复用原图） */
    private String thumbnailUrl(Long userId, File file, String extension, String originalUrl) {
        if ("gif".equals(extension) || "svg".equals(extension)) {
            return originalUrl;
        }
        String thumbnailObject = IdUtil.thumbnailObject(userId, file.getId());
        if (!storageService.objectExists(thumbnailObject)) {
            try (InputStream input = storageService.download(file.getObjectName())) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Thumbnails.of(input).size(500, 500).outputFormat("jpg").toOutputStream(output);
                byte[] bytes = output.toByteArray();
                storageService.upload(thumbnailObject,
                        new java.io.ByteArrayInputStream(bytes), bytes.length, "image/jpeg");
            } catch (IOException e) {
                log.warn("Generate thumbnail failed: fileId={}", file.getId(), e);
                return originalUrl;
            }
        }
        return storageService.generateDownloadUrl(thumbnailObject, adminSettingsService.getDownloadLinkTtlMinutes());
    }
}
