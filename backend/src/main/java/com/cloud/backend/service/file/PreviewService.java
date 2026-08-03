package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.entity.File;

/**
 * 预览服务 —— 图片/视频/音频/PDF/文本，Office 仅下载（本期不做在线预览）。
 * 图片额外生成缩略图（Thumbnailator），预览访问走 presigned URL。
 */
public interface PreviewService {

    FilePreviewResponse preview(Long userId, Long fileId);

    /** 按已鉴权文件对象预览（团队文件等已由调用方校验归属） */
    FilePreviewResponse previewFile(Long userId, File file);

    /** 管理员后台预览（不受禁用限制，可预览被禁文件，用于决定解禁） */
    FilePreviewResponse previewFileForAdmin(File file);
}
