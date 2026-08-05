package com.cloud.backend.service.file;

import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.entity.File;

/**
 * 预览服务 —— 图片/视频/音频/PDF/文本，Office 仅下载（本期不做在线预览）。
 * 图片额外生成缩略图（Thumbnailator），预览访问走 presigned URL。
 *
 * 修改指引：
 * - 【习惯】想改"预览类型判定（按扩展名映射 IMAGE/VIDEO/AUDIO/PDF/TEXT/UNSUPPORTED）" → preview()/previewFile()
 *   对应 PreviewServiceImpl.previewContent() 分支与 FileUtil.isImage()/isText() 白名单；
 *   改动影响各类文件能否预览及返回类型
 * - 【习惯】想改"禁用/对象级禁用文件的预览拦截" → previewFile() 中 FileStatus.DISABLED 与
 *   disabledObjectMapper.countBlocked() 校验；改动影响用户端可预览范围（previewFileForAdmin 不受限，
 *   供管理员决定解禁）
 * - 【习惯】想改"文本预览大小上限" → previewContent() 中 fileProperties.getPreviewTextMaxSize()；
 *   改动影响 TEXT 直读超限回落行为
 * - 【习惯】想改"缩略图生成（尺寸/格式/是否复用）" → thumbnailUrl() 中 Thumbnailator size(500,500) 与 objectExists
 *   复用逻辑（gif/svg 直接回原图）；改动影响 MinIO 缩略图占用与首次生成耗时
 * - 【习惯】想改"预览/缩略图链接有效期" → adminSettingsService.getDownloadLinkTtlMinutes()；改动影响前端直连有效期
 * - 【习惯】新增方法 → 需同步实现类 PreviewServiceImpl 及 FileController/AdminFileController/ShareServiceImpl 调用方
 */
public interface PreviewService {

    FilePreviewResponse preview(Long userId, Long fileId);

    /** 按已鉴权文件对象预览（团队文件等已由调用方校验归属） */
    FilePreviewResponse previewFile(Long userId, File file);

    /** 管理员后台预览（不受禁用限制，可预览被禁文件，用于决定解禁） */
    FilePreviewResponse previewFileForAdmin(File file);
}
