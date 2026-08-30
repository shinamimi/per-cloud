package com.cloud.backend.service.file.impl;

import com.cloud.backend.config.MinioProperties;
import com.cloud.backend.service.file.StorageService;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class StorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioClient presignMinioClient;
    private final MinioProperties properties;

    public StorageServiceImpl(MinioClient minioClient, MinioClient presignMinioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.presignMinioClient = presignMinioClient;
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
            // presign 专用 client（endpoint=public-url），保证签名 host 与浏览器访问地址一致
            return presignMinioClient.getPresignedObjectUrl(args);
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