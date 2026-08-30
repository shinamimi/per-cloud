package com.cloud.backend.service.admin;

import com.cloud.backend.dto.AdminFileQuery;
import com.cloud.backend.dto.Page;
import com.cloud.backend.dto.admin.AdminFileResponse;
import com.cloud.backend.dto.admin.AdminRecycleResponse;
import com.cloud.backend.dto.file.FilePreviewResponse;
import com.cloud.backend.enums.DisableScope;
import com.cloud.backend.enums.FileStatus;

import java.util.List;

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
