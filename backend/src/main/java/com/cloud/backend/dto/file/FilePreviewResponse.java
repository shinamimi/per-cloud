package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 预览响应。
 * type: IMAGE / VIDEO / AUDIO / PDF / TEXT / UNSUPPORTED
 * url：图片/视频/音频/PDF 的直接预览地址（presigned，10 分钟有效）
 * thumbnailUrl：图片缩略图（列表/详情用）
 * content：文本类型时返回文件内容
 *
 * 修改指引：
 * - 【习惯】修改 type            → String type；取值 IMAGE / VIDEO / AUDIO / PDF / TEXT / UNSUPPORTED，前端按此选择预览组件；
 *                         新增类型需同步 PreviewService 判定逻辑与前端组件映射
 * - 【习惯】修改 url             → String url；直接预览地址（presigned，10 分钟有效），过期后需重新请求预览接口
 * - 【习惯】修改 thumbnailUrl    → String thumbnailUrl；图片缩略图地址（列表/详情用）
 * - 【习惯】修改 content         → String content；仅 TEXT 类型返回文件内容，改动需注意文本大小上限（previewTextMaxSize），
 *                         超限文件按配置截断
 * - 【习惯】修改 name / size     → 文件名与文件大小（单位：字节），前端展示用
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
