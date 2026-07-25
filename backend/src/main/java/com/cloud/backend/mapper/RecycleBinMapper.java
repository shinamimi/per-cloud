package com.cloud.backend.mapper;

import com.cloud.backend.entity.RecycleBin;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecycleBinMapper {

    int insert(RecycleBin recycleBin);

    int deleteById(Long id);

    List<RecycleBin> findByUserId(Long userId);
}