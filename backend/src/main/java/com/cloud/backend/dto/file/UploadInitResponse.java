package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 初始化分片上传响应。
 *
 * 修改指引：
 * - 【统一】修改 uploadId        → String uploadId；上传会话唯一标识，后续 chunk / merge / progress 接口均以它为入参，
 *                         改动字段名需同步前端上传流程；改名需同步前端上传流程与 UploadServiceImpl 会话管理
 * - 【统一】修改 chunkSize       → long chunkSize；分片大小，单位：字节；小文件（≤smallFileThreshold）为文件大小单分片，
 *                         大文件取配置 chunkSize；前端按此切分分片，切错会导致 merge 分片数不匹配；改分片边界需同步前端分片逻辑与 UploadServiceImpl
 * - 【统一】修改 totalChunks     → int totalChunks；分片总数 = ceil(fileSize / chunkSize)；前端分片序号范围 1..totalChunks
 *                         （服务端校验 seq ∈ [1, totalChunks]，越界拒绝），注意序号从 1 开始非 0；改分片边界需同步前端分片逻辑与 UploadServiceImpl
 */
@Data
public class UploadInitResponse {

    private String uploadId;
    private long chunkSize;
    private int totalChunks;
}
