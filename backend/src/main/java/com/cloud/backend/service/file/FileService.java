package com.cloud.backend.service.file;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.entity.File;

import java.util.List;

/**
 * 文件/目录领域服务 —— 用户个人空间（teamId=0）的文件 CRUD、分页列表、目录树、重命名/移动/复制、
 * 删除到回收站。
 *
 * 设计思路：
 * - 目录与文件统一 t_file 表模型（type 区分），树形通过 parentId 组织（根为 0）
 * - 同名自动加后缀"（2）"（与主流网盘一致），唯一索引兜底并发竞态
 * - 删除为逻辑删除：status=DELETED + 写回收站记录，保留期后定时任务物理清理
 * - 删除/恢复均释放/占用量化配额（原子 used_space 更新）
 *
 * 修改指引：
 * - 【习惯】想改"同名冲突命名规则（自动加（2）后缀）" → resolveUniqueName()/FileUtil.resolveUniqueName 与
 *   rename()/move()/copy()/createDirectory() 调用点；唯一索引仍兜底并发竞态，改动影响目录内名称唯一性
 * - 【习惯】想改"删除到回收站流程（子树置 DELETED + tombstone 顶层名 + 写 t_recycle_bin + 释放配额）" →
 *   deleteToRecycle()/tombstoneName()；改动影响回收站记录与配额释放时机
 * - 【习惯】想改"回收站保留天数" → deleteToRecycle() 中 adminSettingsService.getRecycleBinDays()；
 *   改动影响 expireTime 与定时清理点
 * - 【习惯】想改"复制语义（目录递归复制、文件共享对象引用 +1、按总大小扣配额）" → copy()/copyNode()；
 *   改动影响配额占用与对象存储引用计数
 * - 【习惯】想改"移动限制（不能移动到自身/子目录、目标目录须归属）" → move()；改动影响目录环的形成可能性
 * - 【习惯】想改"归属校验规则" → getOwnedFile()（teamId=0 个人空间、DELETED 拒绝、DISABLED 仍可见）；
 *   改动影响用户侧可见性边界，禁用在 Download/Preview 服务另行拒绝
 * - 【习惯】事务边界：copy()/deleteToRecycle() 实现为 @Transactional，配额增减与 t_file/t_recycle_bin 写入同事务；
 *   改动须保持原子一致
 * - 【习惯】操作日志：createDirectory() 用 @Log 切面，deleteToRecycle() 内联写 OperationLog；改动影响 OperationLogService
 * - 【习惯】秒传联动：copyNode() 对文件调用 fileHashMapper.incrementRefCount 共享物理对象；
 *   改动影响 FileHashServiceImpl 引用计数（归零才删 MinIO 对象）
 * - 【习惯】新增方法 → 需同步实现类 FileServiceImpl 及 FileController、UploadServiceImpl、DownloadServiceImpl、
 *   PreviewServiceImpl、ShareServiceImpl、TeamFileServiceImpl 等调用方
 */
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
