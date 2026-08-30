package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.entity.File;

public interface PreviewService {

    FilePreviewResponse preview(Long userId, Long fileId);

    /** 按已鉴权文件对象预览（团队文件等已由调用方校验归属） */
    FilePreviewResponse previewFile(Long userId, File file);

    /** 管理员后台预览（不受禁用限制，可预览被禁文件，用于决定解禁） */
    FilePreviewResponse previewFileForAdmin(File file);
}
