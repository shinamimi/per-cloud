package com.cloud.backend.service.file;

/**
 * 秒传索引服务 —— t_file_hash 表 + 引用计数。
 * register：新对象注册（refCount=1），并发已存在则共享引用 +1 并返回共享对象路径；
 * shareRef：秒传/复制命中，共享引用 +1；
 * releaseRef：物理删除时释放引用，归零返回 true（调用方负责物理删除对象）。
 *
 * 修改指引：
 * - 【习惯】想改"秒传命中判定/共享对象策略" → register() 对应 FileHashServiceImpl.register()（命中返回既有
 *   objectName，未命中新建并回退 DuplicateKeyException 共享并发方对象）；改动影响去重生效范围与并发一致性
 * - 【习惯】想改"引用计数增减时机" → register() 命中/并发回退 +1、shareRef() 秒传/复制 +1、releaseRef() 物理删除 -1；
 *   改动影响 MinIO 对象何时真正被删（归零才删）
 * - 【习惯】想改"引用归零判定与清理" → releaseRef() 的 decrementRefCount 返回值判定与 deleteByHash()；
 *   改动影响物理对象的删除与复用
 * - 【习惯】并发注意：引用计数依赖 file_hash.ref_count 的原子 SQL（incrementRefCount/decrementRefCount），
 *   勿改成"先查后改"的读改写，否则并发上传/删除会错乱
 * - 【习惯】新增方法 → 需同步实现类 FileHashServiceImpl 及 UploadServiceImpl/RecycleBinServiceImpl/
 *   AdminFileServiceImpl 等调用方
 */
public interface FileHashService {

    String register(String fileHash, String objectName, long size, String mimeType);

    void shareRef(String fileHash);

    boolean releaseRef(String fileHash);
}
