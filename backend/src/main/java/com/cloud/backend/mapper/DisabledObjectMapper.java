package com.cloud.backend.mapper;

import com.cloud.backend.entity.DisabledObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对象级禁用 Mapper —— t_disabled_object 表。
 * 禁用落在内容 hash 维度：
 * 全站禁（scope=1）与仅用户（scope=2），秒传/下载/预览/上传统一校验。
 *
 * 修改指引：
 * - 【习惯】增删禁用记录         → insert / deleteByHashAndScopeAndUser（XML：src/main/resources/mapper/DisabledObjectMapper.xml）；
 *                          scope、user_id 参与唯一索引 uk_hash_scope_user(file_hash, scope, user_id)，
 *                          改字段名或 scope 取值需同步数据库 DDL，否则重复插入唯一键冲突；
 *                          启用文件时 Service 先删除对应记录再重放剩余禁用
 * - 【习惯】查询某 hash 的禁用记录 → findByHash（XML 同上）；返回该 hash 全部禁用记录（含全站 scope=1 与仅用户 scope=2），
 *                          启用（恢复文件）时 Service 据此重算文件状态，改返回语义需同步 XML
 * - 【习惯】校验是否命中禁用      → countBlocked（XML 同上）；命中条件 scope=1 或（scope=2 且 user_id 匹配），
 *                          返回命中行数（>0 即命中），改命中规则需同步 Service 层秒传/下载/预览/上传拦截判定
 */
@Mapper
public interface DisabledObjectMapper {

    int insert(DisabledObject record);

    /** 该 hash 的全部禁用记录（启用时重算文件状态用） */
    List<DisabledObject> findByHash(String fileHash);

    /** 指定范围/用户的禁用记录（启用时删除） */
    int deleteByHashAndScopeAndUser(@Param("fileHash") String fileHash,
                                    @Param("scope") int scope,
                                    @Param("userId") long userId);

    /** 该用户是否被对象级禁用命中：全站禁（scope=1）或 仅该用户（scope=2+userId） */
    int countBlocked(@Param("fileHash") String fileHash, @Param("userId") Long userId);
}
