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
 *
 * 修改指引：
 * - 【习惯】想改"默认桶" → StorageServiceImpl 中 MinioProperties.getBucket()；改动影响全部对象读写落点
 * - 【习惯】想改"上传/下载/删除的错误语义与流处理" → upload()/download()/delete()/copyObject()/objectExists()/
 *   getObjectInfo() 中 MinIO SDK 调用与 RuntimeException 包装；改动影响各业务（上传/下载/回收站清理）的成败与异常
 * - 【习惯】想改"预签名 URL 有效期" → generateDownloadUrl() 的 expiryInMinutes 参数（调用方传入管理员配置）；
 *   改动影响下载/预览直链可用时长
 * - 【习惯】想改"底层存储实现" → 新增实现类实现本接口（如 S3/OSS），替换 Spring 装配；改动须保证 upload/download/
 *   delete/copyObject/listObjects/bucketExists/createBucket 等语义一致
 * - 【习惯】新增方法 → 需同步实现类 StorageServiceImpl 及 UploadServiceImpl/DownloadServiceImpl/PreviewServiceImpl/
 *   RecycleBinServiceImpl 等调用方
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