package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.MinioProperties;
import com.cloud.backend.service.file.StorageService;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储实现。
 *
 * 设计思路：
 * 基于 MinIO Java SDK，封装了文件的上传、下载、删除、复制、预签名 URL 等操作。
 * 所有操作使用默认桶（properties.getBucket()），简化调用。
 *
 * 修改指引：
 * - 【习惯】想改"默认桶名" → properties.getBucket() 与 MinioProperties 配置（yml minio.bucket）；
 *   改动影响所有对象读写/预签名 URL 的目标桶，需同步保证桶已创建
 * - 【习惯】想改"操作失败语义（当前抛 RuntimeException）" → 各方法 catch 块；改动影响上游异常处理
 *   （如 UploadServiceImpl merge 的兜底补偿、DownloadServiceImpl 转业务异常）
 * - 【习惯】想改"预签名 URL 有效期单位/方法" → generateDownloadUrl() 的 Method.GET 与 expiry(分钟)；
 *   改动影响所有直链下载/预览链接的可用时长
 * - 【习惯】想改"对象存在判定" → objectExists()（statObject 成功为存在，异常为不存在）与
 *   getObjectInfo() 的 ETag 校验用途；改动影响断点续传幂等判断与 MD5 校验
 * - 【习惯】副作用说明：本类直接操作 MinIO（网络 I/O），无事务可言；涉及 upload/delete/copy 的调用方
 *   须自行保证数据库记录与对象存储的最终一致（如秒传引用归零才删）
 * - 【习惯】与接口联动：本类实现 StorageService，改签名/行为须同步接口契约及 UploadServiceImpl、
 *   DownloadServiceImpl、PreviewServiceImpl、RecycleBinServiceImpl、AdminFileServiceImpl 等调用方
 */
@Service
public class StorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public StorageServiceImpl(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String upload(String objectName, InputStream inputStream, long size, String contentType) {
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build();
        try {
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed: " + objectName, e);
        }
        return objectName;
    }

    @Override
    public InputStream download(String objectName) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .build();
        try {
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new RuntimeException("MinIO download failed: " + objectName, e);
        }
    }

    @Override
    public void delete(String objectName) {
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .build();
        try {
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new RuntimeException("MinIO delete failed: " + objectName, e);
        }
    }

    @Override
    public String generateDownloadUrl(String objectName, int expiryInMinutes) {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .method(Method.GET)
                .expiry(expiryInMinutes, TimeUnit.MINUTES)
                .build();
        try {
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw new RuntimeException("MinIO generate presigned URL failed: " + objectName, e);
        }
    }

    @Override
    public void copyObject(String sourceObjectName, String destObjectName) {
        CopySource source = CopySource.builder()
                .bucket(properties.getBucket())
                .object(sourceObjectName)
                .build();
        CopyObjectArgs args = CopyObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(destObjectName)
                .source(source)
                .build();
        try {
            minioClient.copyObject(args);
        } catch (Exception e) {
            throw new RuntimeException("MinIO copy failed: " + sourceObjectName + " -> " + destObjectName, e);
        }
    }

    @Override
    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取对象元信息（大小、Content-Type、ETag）
     * ETag 可以用作文件 MD5 校验
     */
    @Override
    public ObjectInfo getObjectInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
            return new ObjectInfo(stat.size(), stat.contentType(), stat.etag());
        } catch (Exception e) {
            throw new RuntimeException("MinIO stat failed: " + objectName, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO create bucket failed: " + bucketName, e);
        }
    }

    @Override
    public java.util.List<String> listObjects(String prefix) {
        java.util.List<String> names = new java.util.ArrayList<>();
        try {
            Iterable<Result<io.minio.messages.Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(properties.getBucket())
                            .prefix(prefix)
                            .recursive(true)
                            .build());
            for (Result<io.minio.messages.Item> result : results) {
                names.add(result.get().objectName());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO list objects failed: " + prefix, e);
        }
        return names;
    }
}