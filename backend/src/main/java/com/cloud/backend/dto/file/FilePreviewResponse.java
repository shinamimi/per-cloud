package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 预览响应。
 * type: IMAGE / VIDEO / AUDIO / PDF / TEXT / UNSUPPORTED
 * url：图片/视频/音频/PDF 的直接预览地址（presigned，10 分钟有效）
 * thumbnailUrl：图片缩略图（列表/详情用）
 * content：文本类型时返回文件内容
 */
@Data
public class FilePreviewResponse {

    private String type;
    private String url;
    private String thumbnailUrl;
    private String content;
    private String name;
    private Long size;
}
