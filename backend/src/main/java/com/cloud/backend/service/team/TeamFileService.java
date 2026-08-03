package com.cloud.backend.service.team;

import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.file.DirectoryCreateRequest;
import com.cloud.backend.dto.file.FileNodeResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.dto.file.FileTreeResponse;
import com.cloud.backend.dto.file.RecycleBinResponse;

import java.util.List;

/**
 * 团队文件服务 —— 团队文件管理 + 团队回收站。
 * 同表 + teamId 复用个人文件全链路；上传/秒传复用 /api/files/upload/*（请求体带 teamId）。
 * 权限矩阵：MEMBER 只能改/删自己上传的文件，ADMIN 可改/删团队所有文件；下载/预览所有成员可。
 * 团队文件删除进团队回收站（t_recycle_bin 带 team_id），保留天数可配置。
 */
public interface TeamFileService {

    /** 团队文件列表（分页） */
    Page<FileNodeResponse> listFiles(Long teamId, Long userId, Long parentId, int page, int size);

    /** 团队目录树 */
    List<FileTreeResponse> tree(Long teamId, Long userId);

    /** 创建团队目录 */
    FileNodeResponse createDirectory(Long teamId, Long userId, DirectoryCreateRequest request);

    /** 重命名（MEMBER 只能改自己的） */
    FileNodeResponse rename(Long teamId, Long userId, Long fileId, String name);

    /** 移动（MEMBER 只能移动自己的） */
    FileNodeResponse move(Long teamId, Long userId, Long fileId, Long targetParentId);

    /** 复制（MEMBER 只能复制自己的） */
    FileNodeResponse copy(Long teamId, Long userId, Long fileId, Long targetParentId);

    /** 删除进团队回收站（MEMBER 只能删自己的；ADMIN+ 任意），释放团队配额 */
    void deleteToRecycle(Long teamId, Long userId, Long fileId);

    /** 下载：生成 presigned URL（成员均可） */
    String getDownloadUrl(Long teamId, Long userId, Long fileId);

    /** 预览（成员均可） */
    FilePreviewResponse preview(Long teamId, Long userId, Long fileId);

    /* ==================== 团队回收站 ==================== */

    List<RecycleBinResponse> recycleBin(Long teamId, Long userId);

    /** 恢复（权限同文件管理权限），占用团队配额 */
    void restore(Long teamId, Long userId, Long recycleId);

    /** 彻底删除（权限同文件管理权限） */
    void purge(Long teamId, Long userId, Long recycleId);

    /* ==================== 管理端（AdminTeamController 使用） ==================== */

    /** 管理端查看团队文件（不要求调用者是该团队会员） */
    Page<FileNodeResponse> adminListFiles(Long teamId, Long parentId, int page, int size);

    /** 管理端查看团队回收站 */
    List<RecycleBinResponse> adminRecycleBin(Long teamId);

    /** 管理端物理清除团队回收站记录 */
    void adminPurge(Long teamId, Long recycleId);
}
