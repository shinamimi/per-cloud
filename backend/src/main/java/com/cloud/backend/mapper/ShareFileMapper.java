package com.cloud.backend.mapper;

import com.cloud.backend.entity.ShareFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareFileMapper {

    int insert(ShareFile shareFile);

    /** 批量插入快照节点 */
    int batchInsert(@Param("shareId") Long shareId, @Param("nodes") List<ShareFile> nodes);

    List<ShareFile> findByShareId(Long shareId);

    List<ShareFile> findByIds(@Param("shareId") Long shareId, @Param("ids") List<Long> ids);

    int deleteByShareId(Long shareId);
}
