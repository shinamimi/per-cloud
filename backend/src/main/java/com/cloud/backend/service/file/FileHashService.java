package com.cloud.backend.service.file;

/**
 * 秒传索引服务 —— t_file_hash 表 + 引用计数。
 * register：新对象注册（refCount=1），并发已存在则共享引用 +1 并返回共享对象路径；
 * shareRef：秒传/复制命中，共享引用 +1；
 * releaseRef：物理删除时释放引用，归零返回 true（调用方负责物理删除对象）。
 */
public interface FileHashService {

    String register(String fileHash, String objectName, long size, String mimeType);

    void shareRef(String fileHash);

    boolean releaseRef(String fileHash);
}
