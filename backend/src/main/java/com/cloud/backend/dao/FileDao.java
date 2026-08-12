package com.cloud.backend.dao;

import com.cloud.backend.dto.FileQuery;
import com.cloud.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 文件复杂查询 DAO —— 搜索（文件名 LIKE + 类型过滤 + 分页）。
 *
 * 修改指引：
 * - 【统一】新增查询方法            → 在此接口声明方法，并在 resources/mapper/FileDao.xml 编写对应 SQL；
 *                             改后需同步 resources/mapper/FileDao.xml 中对应 SQL（id 与方法名一致）
 * - 【统一】修改搜索过滤条件        → searchPage / count 的 SQL 在 FileDao.xml；条件来自 FileQuery 字段，改动影响文件搜索/筛选结果；
 *                             改后需同步 FileQuery 字段与 FileDao.xml 中对应 SQL
 * - 【统一】修改分页方式            → searchPage / count 的 SQL；当前为 LIMIT 分页，改动需同步调用方与总条数统计；
 *                             改后需同步调用方（分页参数）与 count 总条数统计
 * - 【统一】修改返回实体            → 方法返回值 List&lt;File&gt; / long；改动需同步 Service 层取用字段；
 *                             改后需同步 Service 层取用字段
 */
@Mapper
public interface FileDao {

    List<File> searchPage(FileQuery query);

    long count(FileQuery query);
}
