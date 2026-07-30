package com.cloud.backend.service.file;

import com.cloud.backend.entity.RecycleBin;

import java.util.List;

/**
 * 回收站服务接口。
 * 文件删除时调用 save 移入回收站，恢复时 removeById 并重新插入 t_file。
 */
public interface RecycleBinService {

    RecycleBin save(RecycleBin recycleBin);

    int removeById(Long id);

    List<RecycleBin> listByUserId(Long userId);
}