package com.cloud.backend.service.file;

import com.cloud.backend.entity.RecycleBin;

import java.util.List;

/**
 * 回收站服务接口。
 * 文件删除时调用 save 移入回收站，恢复时 removeById 并重新插入 t_file。
 * purge：物理删除（立即/过期定时）；恢复：restore。
 */
public interface RecycleBinService {

    RecycleBin save(RecycleBin recycleBin);

    int removeById(Long id);

    List<RecycleBin> listByUserId(Long userId);

    /** 恢复：状态置回正常 + 删除回收站记录 + 重新占用量化空间（不足则拒绝） */
    void restore(Long userId, Long recycleId);

    /** 立即物理删除：释放对象（引用归零时）与记录，目录递归 */
    void purge(Long userId, Long recycleId);

    /** 定时清理：删除所有已过期记录（30 天） */
    void purgeExpired();

    /** 物理清理单条记录（权限已由调用方校验；团队回收站复用） */
    void purgeRecord(RecycleBin record);
}
