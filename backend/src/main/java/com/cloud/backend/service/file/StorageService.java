package com.cloud.backend.service.file;

import java.io.InputStream;

public interface StorageService {

    String upload(String objectName, InputStream inputStream, long size, String contentType);

    InputStream download(String objectName);

    void delete(String objectName);

    /** 生成预签名下载 URL，带过期时间 */
    String generateDownloadUrl(String objectName, int expiryInMinutes);

    void copyObject(String sourceObjectName, String destObjectName);

    boolean objectExists(String objectName);

    ObjectInfo getObjectInfo(String objectName);

    /** 列出指定前缀下的全部对象名（分页内部处理） */
    java.util.List<String> listObjects(String prefix);

    boolean bucketExists(String bucketName);

    void createBucket(String bucketName);

    record ObjectInfo(
            long size,
            String contentType,
            String etag
    ) {}
}