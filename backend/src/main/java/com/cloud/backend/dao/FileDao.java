package com.cloud.backend.dao;

import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FileDao {

    List<File> search(FileQuery query);

}
