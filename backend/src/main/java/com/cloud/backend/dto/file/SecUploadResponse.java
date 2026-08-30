package com.cloud.backend.dto.file;

import lombok.Data;

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
