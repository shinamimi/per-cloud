package com.cloud.backend.service.share;

import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.share.*;
import com.cloud.backend.entity.Share;

import java.util.List;

/**
 * 分享服务 —— 用户侧分享管理 + 访客访问。
 * 访客接口不要求登录（SecurityConfig permitAll），转存除外（需登录）。
 *
 * 修改指引：
 * - 【习惯】想改"创建分享（快照锁定/有效期/提取码/下载策略/每文件分享数上限）" → createShare() 对应
 *   ShareServiceImpl.createShare()（@Transactional + @Log CREATE_SHARE；目录 BFS 快照 t_share_file）；
 *   改动影响分享内容快照与分享数/有效期上限
 * - 【习惯】想改"取消/删除分享" → cancelShare()/deleteShareRecord()/adminCancelShare()/adminDeleteShare()
 *   （清快照 + 清提取码验证标记 + 写日志，删除为物理删除）；改动影响访客可见性与 Redis 标记清理
 * - 【习惯】想改"提取码验证（错误 5 次锁定 / 通过 24h 标记）" → verifyPassword() 中 Redis 计数与
 *   SHARE_PWD_FAIL_PREFIX/SHARE_PWD_OK_PREFIX、PASSWORD_FAIL_LIMIT/PASSWORD_OK_TTL；改动影响访客验证门槛
 *   与锁定时长
 * - 【习惯】想改"下载计数（原子 UPDATE 防超限 + 同 IP 60 秒去重）" → getShareDownloadUrl()/batchDownload() 对应
 *   countDownload()/incrementDownloadCountIfAllowed() 与 dedupKey；改动影响下载次数统计与 EXHAUSTED 状态触发
 * - 【习惯】想改"访客下载/预览（回查原文件可用性 + 对象级禁用拦截）" → getShareDownloadUrl()/previewShareFile() 对应
 *   requireShareableFile()；改动影响访客可下载/可预览范围
 * - 【习惯】想改"转存到个人空间" → saveShareFiles()（@Transactional；快照树建目录 + 逐文件复用 uploadService.sec()
 *   引用 +1；原文件失效/被禁用整体中止）；改动影响转存语义与配额占用
 * - 【习惯】安全提示：访客接口不要求登录，改动权限/校验须同步 Controller 与 SecurityConfig
 * - 【习惯】新增方法 → 需同步实现类 ShareServiceImpl 与 ShareController 调用方
 */
public interface ShareService {

    /* ==================== 基础 CRUD（原有） ==================== */

    Share create(Share share);

    Share findByToken(String shareToken);

    Share findById(Long id);

    List<Share> listByUserId(Long userId);

    int update(Share share);

    int removeById(Long id);

    List<Share> findAll();

    void adminCancelShare(Long id);

    /** 管理员切换分享下载开关（允许/禁止下载，立即生效） */
    void adminSetAllowDownload(Long id, boolean allowDownload);

    /** 管理员删除分享记录（物理删除 t_share + 快照） */
    void adminDeleteShare(Long id);

    /* ==================== 用户侧分享管理 ==================== */

    /** 创建分享：单文件或文件夹（快照锁定） */
    Share createShare(Long userId, ShareCreateRequest request);

    /** 我的分享列表（含根节点名） */
    List<ShareResponse> listShares(Long userId);

    /** 修改有效期（延长/缩短，永久 ↔ 天数） */
    void updateExpire(Long userId, Long shareId, ShareUpdateRequest request);

    /** 用户取消分享（删除快照） */
    void cancelShare(Long userId, Long shareId);

    /** 用户删除分享记录（物理删除 t_share + 快照，彻底移除） */
    void deleteShareRecord(Long userId, Long shareId);

    /* ==================== 访客访问 ==================== */

    /** 获取分享信息（未验证提取码也可获取元数据，供前端弹密码框） */
    GuestShareInfoResponse getAccessInfo(String token);

    /** 验证提取码（Redis 限次：错误 5 次锁定） */
    void verifyPassword(String token, String password);

    /** 分享文件树（平铺快照节点，提取码未验证时拒绝） */
    List<ShareFileNodeResponse> getShareFiles(String token);

    /** 单文件下载（下载次数 +1） */
    String getShareDownloadUrl(String token, Long snapshotId);

    /** 分享文件预览（不计数） */
    FilePreviewResponse previewShareFile(String token, Long snapshotId);

    /** 批量打包下载（一次下载动作计数 +1） */
    BatchDownloadResponse batchDownload(String token, List<Long> snapshotIds);

    /** 批量打包任务查询 */
    BatchDownloadResponse getBatchTask(String taskId);

    /** 转存到个人空间（秒传引用 +1，需登录） */
    void saveShareFiles(Long userId, String token, List<Long> snapshotIds);
}
