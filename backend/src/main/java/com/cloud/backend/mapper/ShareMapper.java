package com.cloud.backend.mapper;

import com.cloud.backend.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

import java.util.List;

@Mapper
public interface ShareMapper {

    int insert(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> findByUserId(Long userId);

    int update(Share share);

    int deleteById(Long id);

    List<Share> findAll();
}