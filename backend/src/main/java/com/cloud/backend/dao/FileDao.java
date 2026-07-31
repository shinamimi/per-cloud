package com.cloud.backend.dao;

import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 文件复杂查询 DAO —— 搜索（文件名 LIKE + 类型过滤 + 分页）。
 */
@Mapper
public interface FileDao {

    List<File> searchPage(FileQuery query);

    long count(FileQuery query);
}
