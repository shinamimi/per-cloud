package com.cloud.backend.service.admin;

import com.cloud.backend.dto.AdminFileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.admin.AdminFileResponse;
import com.cloud.backend.dto.admin.AdminRecycleResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;

import java.util.List;

/**
 * 管理端全局文件管控服务（全部仅 ADMIN+）。
 *
 * 核心语义：
 * - 禁用/启用：禁用 = 用户仍可见文件，但不可下载/预览/分享
 * - 删除：管理员删除进全局回收站（deleted_by=1），与用户私有回收站隔离
 * - 恢复：按删除来源恢复；个人空间校验用户配额，团队空间校验团队配额
 * - 彻底删除：复用个人回收站物理清理（递归 + 秒传引用归零 + MinIO 删除）
 *
 * 修改指引：
 * - 【习惯】想改"全局列表/详情（个人+团队统一筛选、归属/禁用来源展示）" → page()/detail()/detailEntity() 对应
 *   AdminFileServiceImpl.page()/detail() 及 fileMapper.adminCount/adminPage 动态 SQL、fillOwners()/fillDisabledScope()；
 *   改动影响管理端文件列表的可见范围与展示
 * - 【习惯】想改"禁用/启用状态流转（FileStatus NORMAL/DISABLED）" → changeStatus() 对应
 *   AdminFileServiceImpl.changeStatus()/disableObject()/enableObject()；有 hash 走对象级 DisabledObject 记录
 *   （GLOBAL=全站禁、USER=仅用户），无 hash 直接改文件状态；改动影响用户侧可见性（status != 0 仍可见）
 *   与下载/预览是否被拒
 * - 【习惯】想改"删除进全局回收站（tombstone 顶层名 + 递归子树置 DELETED + 写 t_recycle_bin.deleted_by=1 + 释放配额）" →
 *   deleteToGlobalRecycleBin()；改动影响回收站记录、唯一索引占用与配额释放时机
 * - 【习惯】想改"全局回收站恢复（递归恢复子树 + 用户/团队配额校验 + 同名唯一化 + 父目录可用校验）" → restore()；
 *   改动影响恢复后目录结构完整性与配额是否超限
 * - 【习惯】想改"彻底删除（支持批量）" → purge() 委托 RecycleBinService.purgeRecord()（递归 + 秒传引用归零 + MinIO 删除）；
 *   改动影响秒传引用计数与存储对象清理
 * - 【习惯】事务边界：changeStatus()/deleteToGlobalRecycleBin()/restore()/purge() 实现均为 @Transactional，
 *   配额增减与状态、回收站记录同事务；改动隔离/传播须保持原子一致
 * - 【习惯】操作日志：上述写方法内联写 OperationLog（DISABLE_FILE/ENABLE_FILE/DELETE_FILE/RESTORE_FILE）；
 *   改动记录时机/内容会影响 OperationLogService 与日志管理页
 * - 【习惯】新增方法 → 需同步实现类 AdminFileServiceImpl 与 AdminFileController
 */
public interface AdminFileService {

    /** 全局文件列表（个人+团队统一，筛选+分页） */
    Page<AdminFileResponse> page(AdminFileQuery query);

    /** 文件详情（含所属用户/团队显示名） */
    AdminFileResponse detail(Long id);

    /** 文件详情（原始实体，管理端下载/预览用，不做禁用校验） */
    com.cloud.backend.entity.File detailEntity(Long id);

    /** 管理端下载 URL（不做禁用校验，管理员可下载禁用文件） */
    String generateDownloadUrl(com.cloud.backend.entity.File file);

    /** 禁用/启用（scope 仅禁用时生效：GLOBAL=全站禁，USER=仅用户） */
    void changeStatus(Long id, FileStatus status, DisableScope scope);

    /** 删除（进全局回收站，支持批量） */
    void deleteToGlobalRecycleBin(List<Long> ids);

    /** 全局回收站列表 */
    List<AdminRecycleResponse> globalRecycleBin();

    /** 恢复（个人空间校验用户配额，团队空间校验团队配额） */
    void restore(Long recycleId);

    /** 彻底删除（支持批量，复用物理清理） */
    void purge(List<Long> recycleIds);
}
