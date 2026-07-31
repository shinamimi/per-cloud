package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 秒传响应：instant=true 秒传完成（file 为新建记录），false 需继续分片上传。
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
