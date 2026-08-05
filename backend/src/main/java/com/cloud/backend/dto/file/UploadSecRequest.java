package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 秒传请求（全站 SHA256 命中则引用现有对象，不上传）。
 *
 * 修改指引：
 * - 【习惯】修改 fileHash        → String fileHash；文件 SHA-256，请求体字段名对应 POST /api/files/upload/sec 入参；
 *                         命中全站共享索引则零复制建记录（引用计数 +1）；前后端必须使用同一哈希算法，哈希错误可能误命中
 * - 【习惯】修改 fileName        → String fileName；文件名，服务层限长 1-255，超出 400
 * - 【习惯】修改 fileSize        → Long fileSize；文件大小，单位：字节，必须与秒传索引中登记的大小一致，
 *                         不一致时服务端抛 UPLOAD_INVALID（防哈希碰撞误复用）
 * - 【习惯】修改 parentId        → Long parentId；父目录 id，根目录传 0
 * - 【习惯】修改 teamId          → Long teamId；团队空间预留（本期仅个人空间），null/<=0 归一化为个人空间
 * - 【习惯】修改校验注解 @NotBlank/@NotNull/@Positive → 改动影响接口契约，前端必填与非空/正数约束随之变化
 */
@Data
public class UploadSecRequest {

    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于 0")
    private Long fileSize;

    @NotNull(message = "父目录不能为空")
    private Long parentId;

    /** 团队空间预留（本期仅个人空间） */
    private Long teamId;
}
