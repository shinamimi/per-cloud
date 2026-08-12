package com.cloud.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 初始化分片上传请求。
 *
 * 修改指引：
 * - 【统一】修改 fileName        → String fileName；文件名，请求体字段名对应 POST /api/files/upload/init 入参；服务层限长 1-255，超出 400；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改 fileSize        → Long fileSize；文件大小，单位：字节，@Positive 必须大于 0；参与配额校验、单文件上限与分片计算，
 *                         填错会导致配额校验/分片切分错乱；改后需同步前端上传逻辑与 UploadServiceImpl 配额/分片校验
 * - 【统一】修改 fileHash        → String fileHash；文件 SHA-256，前端需先算完整文件哈希；服务端 merge 边传边算比对，
 *                         不一致则合并失败，哈希算法改动需前后端同步；改后需同步前端哈希计算与 UploadServiceImpl 合并比对
 * - 【统一】修改 parentId        → Long parentId；父目录 id，根目录传 0；服务层校验父目录存在、为正常目录且空间归属一致；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改 teamId          → Long teamId；团队空间预留（本期仅个人空间），null/<=0 归一化为个人空间；改名需同步前端 API 层与 Service 组装
 * - 【统一】修改校验注解 @NotBlank/@NotNull/@Positive → 改动影响接口契约，前端必填与非空/正数约束随之变化；改后需同步前端必填/非空/正数约束
 */
@Data
public class UploadInitRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于 0")
    private Long fileSize;

    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    @NotNull(message = "父目录不能为空")
    private Long parentId;

    /** 团队空间预留（本期仅个人空间） */
    private Long teamId;
}
