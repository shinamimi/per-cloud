package com.cloud.backend.dto.file;

import lombok.Data;

import java.util.List;

/**
 * 断点续传进度响应：已上传分片序号列表。
 *
 * 修改指引：
 * - 【统一】修改 uploadId        → String uploadId；对应 GET /api/files/upload/progress/{uploadId} 路径参数；改名需同步前端续传流程与 UploadServiceImpl 进度查询
 * - 【习惯】修改 fileName / fileSize / mimeType → 上传文件元信息（fileSize 单位：字节），前端续传时回显
 * - 【统一】修改 uploadedChunks  → List&lt;Integer&gt; uploadedChunks；已上传分片序号列表（升序），序号范围 1..totalChunks；
 *                         前端断点续传据此跳过已传分片，注意序号从 1 开始，不要与数组下标混淆；改分片边界需同步前端断点续传逻辑与 UploadServiceImpl
 * - 【习惯】修改 parentId / teamId → 父目录 id / 团队 id（个人空间为 0），续传时前端用于校验或回显
 */
@Data
public class UploadProgressResponse {

    private String uploadId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private List<Integer> uploadedChunks;
    private Long parentId;
    private Long teamId;
}
