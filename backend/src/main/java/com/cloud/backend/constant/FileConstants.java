package com.cloud.backend.constant;

public interface FileConstants {

    long KB = 1024;
    long MB = KB * 1024;
    long GB = MB * 1024;

    /** 【统一】改后需同步所有读取方（注册初始化配额计算等） */
    long DEFAULT_QUOTA = 10 * GB;
    /** 【统一】改后需同步前端分片上传的分片大小约定 */
    long DEFAULT_CHUNK_SIZE = 10 * MB;
    /** 【统一】改后需同步根目录判定逻辑与所有涉及 parentId 的查询（各文件/分享/团队 Service） */
    long ROOT_PARENT_ID = 0;

    /** 【统一】改后需同步 t_file.category 存量数据、FileUtil.categoryOf 与前端分类过滤 */
    int IMAGE = 0;
    int DOCUMENT = 1;
    int VIDEO = 2;
    int AUDIO = 3;
    int ARCHIVE = 4;
    int OTHER = 5;
}