package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FilePreviewResponse;

/**
 * 预览服务 —— 图片/视频/音频/PDF/文本，Office 仅下载（本期不做在线预览）。
 * 图片额外生成缩略图（Thumbnailator），预览访问走 presigned URL。
 */
public interface PreviewService {

    FilePreviewResponse preview(Long userId, Long fileId);
}
