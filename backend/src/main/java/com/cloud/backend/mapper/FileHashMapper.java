package com.cloud.backend.mapper;

import com.cloud.backend.entity.FileHash;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 秒传索引 Mapper —— t_file_hash 表。
 * findByHash 命中即秒传；refCount 引用计数，归零才物理删除对象。
 *
 * 修改指引：
 * - 【习惯】新增秒传索引        → insert（XML：src/main/resources/mapper/FileHashMapper.xml）；file_hash 命中唯一索引
 *                          uk_hash(file_hash)，改 hash 字段名需同步数据库 DDL；秒传成功后由 Service 调 incrementRefCount 建立引用
 * - 【习惯】秒传命中查询         → findByHash（XML 同上）；按 file_hash 单查（LIMIT 1），命中即复用共享对象实现秒传，
 *                          返回的 object_name 供新文件复用，改字段需同步 XML 与 DDL
 * - 【习惯】调整引用计数         → incrementRefCount / decrementRefCount（XML 同上）；SQL 为 ref_count ± 1 的原子更新，
 *                          防并发覆盖，删除文件/释放引用时由 Service 成对调用，改计数语义需同步 XML
 * - 【习惯】删除秒传索引         → deleteByHash（XML 同上）；需在 ref_count 归零（decrementRefCount 后由 Service 判断）
 *                          且 MinIO 对象删除后再调用，否则遗留孤儿对象或引用未清
 */
@Mapper
public interface FileHashMapper {

    int insert(FileHash fileHash);

    FileHash findByHash(String fileHash);

    int incrementRefCount(String fileHash);

    int decrementRefCount(String fileHash);

    int deleteByHash(String fileHash);

    /** 原子删除：ref_count <= 0 时才删除（替代 TOCTOU 竞态） */
    int deleteIfNoRef(String fileHash);
}
