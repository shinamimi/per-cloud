package com.cloud.backend.service.file;

import java.io.InputStream;

/**
 * 对象存储抽象接口。
 *
 * 设计思路：
 * 抽象出 StorageService 接口，未来可以替换实现（如本地文件系统、阿里云 OSS、AWS S3 等），
 * 业务层只依赖此接口，不感知底层存储。
 *
 * 当前实现：MinIO
 */
public interface StorageService {

    String upload(String objectName, InputStream inputStream, long size, String contentType);

    InputStream download(String objectName);

    void delete(String objectName);

    /** 生成预签名下载 URL，带过期时间 */
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