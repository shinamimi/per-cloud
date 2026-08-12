package com.cloud.backend.constant;

/**
 * 文件相关常量 —— 单位换算、默认配额、分片大小、文件类型分类。
 *
 * 修改指引：
 * - 【习惯】修改单位换算基数        → KB/MB/GB 三个常量；当前为 1024 进制（二进制），改动会影响配额/分片/容量换算的所有展示与计算
 * - 【统一】修改默认配额            → DEFAULT_QUOTA；单位字节（当前 10*GB），影响新用户注册时的初始配额；
 *                             改后需同步所有读取方（注册初始化配额计算等）
 * - 【统一】修改分片大小            → DEFAULT_CHUNK_SIZE；单位字节（当前 10*MB），影响前端分片上传的分片大小约定；
 *                             改后需同步前端分片上传的分片大小约定
 * - 【统一】修改根目录父 ID         → ROOT_PARENT_ID；当前 0，改动会改变根目录判定逻辑，需同步检查涉及 parentId 的查询；
 *                             改后需同步根目录判定逻辑与所有涉及 parentId 的查询（各文件/分享/团队 Service）
 * - 【统一】修改文件类型分类编号     → IMAGE/DOCUMENT/VIDEO/AUDIO/ARCHIVE/OTHER；编号对应 t_file.category 存储值，
 *                             改动需同步存量数据与 FileUtil.categoryOf；改后需同步 t_file.category 存量数据、FileUtil.categoryOf 与前端分类过滤
 * - 【统一】新增文件类型分类        → 在此新增常量，并同步 FileUtil.categoryOf 的映射与前端分类过滤逻辑；
 *                             改后需同步 FileUtil.categoryOf 映射与前端分类过滤逻辑
 */
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