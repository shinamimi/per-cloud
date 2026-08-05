package com.cloud.backend.service.file;

import com.cloud.backend.entity.RecycleBin;

import java.util.List;

/**
 * 回收站服务接口。
 * 文件删除时调用 save 移入回收站，恢复时 removeById 并重新插入 t_file。
 * purge：物理删除（立即/过期定时）；恢复：restore。
 *
 * 修改指引：
 * - 【习惯】想改"恢复流程（递归恢复子树 + 配额校验 + 同名唯一化 + 父目录可用校验）" → restore() 对应
 *   RecycleBinServiceImpl.restore()/restoreRecord()；改动影响恢复后目录结构完整性与配额是否超限
 * - 【习惯】想改"物理删除条件（引用归零才删）" → purge()/purgeRecord()/purgeExpired() → releaseObject()：
 *   有 hash 走 fileHashService.releaseRef() 归零判定，无 hash 直接删 MinIO 对象；改动影响秒传共享对象是否被误删
 * - 【习惯】想改"过期清理（保留天数）" → purgeExpired()（findByExpireTimeBefore + purgeInternal，由定时任务调用）；
 *   改动影响过期记录的物理删除时机与保留策略
 * - 【习惯】想改"递归清理维度" → purgeInternal()：目录按团队维度（findByTeamIdAndParentId）或个人维度
 *   （findByUserIdAndParentId）递归子记录；改动影响跨成员上传的团队目录清理
 * - 【习惯】幂等：purgeInternal() 先按 id 复查记录存在性，避免递归处理中重复删除；改动清理入口须保持该防护
 * - 【习惯】事务：purgeExpired()/purgeRecord() 实现无显式 @Transactional，引用计数与存储删除依赖 FileHashMapper/
 *   StorageService 各自的原子操作；如需整体回滚须补充事务边界
 * - 【习惯】操作日志：restoreRecord() 内联写 RESTORE_FILE 日志；改动影响 OperationLogService
 * - 【习惯】新增方法 → 需同步实现类 RecycleBinServiceImpl 及 FileController、AdminFileServiceImpl、TeamFileServiceImpl
 *   （复用 purgeRecord）等调用方
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
