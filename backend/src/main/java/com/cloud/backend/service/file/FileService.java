package com.cloud.backend.service.file;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.entity.File;

import java.util.List;

public interface FileService {

    File save(File file);

    File findById(Long id);

    List<File> listByUserAndParent(Long userId, Long parentId);

    File findByPath(Long userId, String path);

    int update(File file);

    int removeById(Long id);

    int updateStatus(Long id, Integer status);

    List<File> findAll();

    /** 文件列表（分页，按 parentId 过滤） */
    Page<FileNodeResponse> pageByUserAndParent(Long userId, Long parentId, int page, int size);

    /** 目录树（仅目录节点） */
    List<FileTreeResponse> tree(Long userId);

    /** 创建目录（同名自动加后缀） */
    File createDirectory(Long userId, DirectoryCreateRequest request);

    /** 重命名（仅改数据库 name） */
    FileNodeResponse rename(Long userId, Long fileId, String name);

    /** 移动（仅改数据库 parentId，MinIO 对象不动） */
    FileNodeResponse move(Long userId, Long fileId, Long targetParentId);

    /** 复制（文件引用共享对象；目录递归复制结构） */
    FileNodeResponse copy(Long userId, Long fileId, Long targetParentId);

    /** 移入回收站（逻辑删除 + 写 t_recycle_bin，递归处理子树，配额释放） */
    void deleteToRecycle(Long userId, Long fileId);

    /** 音频列表（分页，category=AUDIO，个人空间）—— 音乐播放器预留接口（file-module.md 十二.10） */
    Page<FileNodeResponse> listAudio(Long userId, int page, int size);

    /** 校验归属并返回文件（不存在或非本人 → FILE_NOT_FOUND） */
    File getOwnedFile(Long userId, Long fileId);

    /** 同名自动加后缀（"（2）""（3）"...），防止唯一索引冲突 */
    String resolveUniqueName(Long userId, Long parentId, String baseName);
}
