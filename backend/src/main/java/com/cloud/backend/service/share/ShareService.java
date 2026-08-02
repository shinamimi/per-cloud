package com.cloud.backend.service.share;

import com.cloud.backend.dto.file.BatchDownloadResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.share.*;
import com.cloud.backend.entity.Share;

import java.util.List;

/**
 * 分享服务 —— 用户侧分享管理 + 访客访问（docs/share-module.md M4）。
 * 访客接口不要求登录（SecurityConfig permitAll），转存除外（需登录）。
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

    /* ==================== 用户侧分享管理 ==================== */

    /** 创建分享：单文件或文件夹（快照锁定） */
    Share createShare(Long userId, ShareCreateRequest request);

    /** 我的分享列表（含根节点名） */
    List<ShareResponse> listShares(Long userId);

    /** 修改有效期（延长/缩短，永久 ↔ 天数） */
    void updateExpire(Long userId, Long shareId, ShareUpdateRequest request);

    /** 用户取消分享（删除快照） */
    void cancelShare(Long userId, Long shareId);

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
