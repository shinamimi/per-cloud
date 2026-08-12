package com.cloud.backend.dto.file;

import com.cloud.backend.entity.RecycleBin;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站记录响应。
 *
 * 修改指引：
 * - 【统一】修改 id              → Long id；回收站记录 id，恢复/彻底删除接口的路径参数（POST /api/files/recycle-bin/{id}/restore、
 *                         DELETE /api/files/recycle-bin/{id}）；改名需同步前端恢复/彻底删除操作与 RecycleBinService
 * - 【习惯】修改 fileId          → Long fileId；原文件 id
 * - 【习惯】修改 originalName    → String originalName；回收前原始文件名，恢复时可能因同名冲突自动追加后缀
 * - 【统一】修改 type            → Integer type；0-文件 1-目录（t_recycle_bin.type TINYINT），恢复时决定建文件还是建目录；改后需同步 t_recycle_bin.type 取值与恢复逻辑
 * - 【习惯】修改 size            → Long size；文件大小，单位：字节，前端展示需换算
 * - 【习惯】修改 deletedTime / expireTime → LocalDateTime 删除时间与到期物理清理时间；前端可展示"还剩 X 天"，到期由定时任务物理清理
 */
@Data
public class RecycleBinResponse {

    private Long id;
    private Long fileId;
    private String originalName;
    private Integer type;
    private Long size;
    private LocalDateTime deletedTime;
    private LocalDateTime expireTime;

    public static RecycleBinResponse from(RecycleBin recycleBin) {
        RecycleBinResponse response = new RecycleBinResponse();
        response.setId(recycleBin.getId());
        response.setFileId(recycleBin.getFileId());
        response.setOriginalName(recycleBin.getOriginalName());
        response.setType(recycleBin.getType());
        response.setSize(recycleBin.getSize());
        response.setDeletedTime(recycleBin.getDeletedTime());
        response.setExpireTime(recycleBin.getExpireTime());
        return response;
    }
}
