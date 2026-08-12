package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 秒传响应：instant=true 秒传完成（file 为新建记录），false 需继续分片上传。
 *
 * 修改指引：
 * - 【统一】修改 instant         → boolean instant；true 表示秒传完成，false 表示哈希未命中需走 upload/init 分片上传；
 *                         前端据此决定是否发起实际上传，改动该字段名/语义需同步前端分叉逻辑；改名/改语义需同步前端分叉逻辑与 UploadServiceImpl 秒传判定
 * - 【统一】修改 file            → FileNodeResponse file；instant=true 时为新建文件记录，false 时为 null；
 *                         前端避免在 miss 时读取 file 字段（防 NPE）；改名需同步前端分叉逻辑与 FileNodeResponse 组装
 */
@Data
public class SecUploadResponse {

    private boolean instant;
    private FileNodeResponse file;

    public static SecUploadResponse hit(FileNodeResponse file) {
        SecUploadResponse response = new SecUploadResponse();
        response.setInstant(true);
        response.setFile(file);
        return response;
    }

    public static SecUploadResponse miss() {
        SecUploadResponse response = new SecUploadResponse();
        response.setInstant(false);
        return response;
    }
}
