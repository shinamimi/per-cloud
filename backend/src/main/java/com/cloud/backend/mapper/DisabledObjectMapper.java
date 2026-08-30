package com.cloud.backend.mapper;

import com.cloud.backend.entity.DisabledObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
