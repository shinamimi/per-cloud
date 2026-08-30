package com.cloud.backend.service.file;

public interface FileHashService {

    String register(String fileHash, String objectName, long size, String mimeType);

    void shareRef(String fileHash);

    boolean releaseRef(String fileHash);
}
