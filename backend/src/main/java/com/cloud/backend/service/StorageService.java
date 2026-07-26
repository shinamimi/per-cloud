package com.cloud.backend.service;

import java.io.InputStream;

public interface StorageService {

    String upload(String objectName, InputStream inputStream, long size, String contentType);

    InputStream download(String objectName);

    void delete(String objectName);

    String generateDownloadUrl(String objectName, int expiryInMinutes);

    void copyObject(String sourceObjectName, String destObjectName);

    boolean objectExists(String objectName);

    ObjectInfo getObjectInfo(String objectName);

    boolean bucketExists(String bucketName);

    void createBucket(String bucketName);

    record ObjectInfo(
            long size,
            String contentType,
            String etag
    ) {}
}