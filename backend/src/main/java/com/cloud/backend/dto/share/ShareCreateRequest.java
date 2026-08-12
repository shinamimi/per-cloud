package com.cloud.backend.dto.share;

import lombok.Data;

/**
 * 创建分享请求 —— POST /api/shares。
 * validType: PERMANENT=永久 / DAYS=按天数（validDays 生效）
 *
 * 修改指引：
 * - 【统一】修改 fileId          → Long fileId；被分享文件/文件夹 id（仅个人空间），目录分享创建时锁定快照（t_share_file）；改名需同步前端创建分享表单与服务层快照锁定
 * - 【统一】修改 validType       → String validType；PERMANENT=永久 / DAYS=按天数（validDays 生效）；
 *                         改动取值需与前端下拉选项及服务端有效期计算保持一致；改后需同步前端下拉选项与服务端有效期计算
 * - 【统一】修改 validDays       → Integer validDays；validType=DAYS 时有效期天数，上限 share.max-valid-days
 *                         （管理员配置），超出服务端 400；改后需同步前端表单与服务端有效期计算（含 share.max-valid-days 上限校验）
 * - 【统一】修改 requirePassword → Boolean requirePassword；true 时 accessPassword 必填，且访客访问需先验证提取码；改后需同步前端表单必填逻辑与服务端访问校验
 * - 【统一】修改 accessPassword  → String accessPassword；提取码，错误限次 5 次（Redis 计数，超限锁定）；改后需同步 Redis 计数键与分享访问校验逻辑
 * - 【统一】修改 allowDownload   → Boolean allowDownload；true=允许下载（maxDownload 可选）/ false=禁止下载只能预览；改后需同步下载计数逻辑与前端下载入口显隐
 * - 【统一】修改 maxDownload     → Integer maxDownload；下载次数限制，0=不限，仅 allowDownload=true 时有效，
 *                         下载计数达限后置 EXHAUSTED；改后需同步下载计数与 EXHAUSTED 状态流转
 * - 【统一】修改 allowSave       → Boolean allowSave；允许转存，false 时访客不可转存到个人空间；改后需同步转存校验与前端转存入口显隐
 */
@Data
public class ShareCreateRequest {

    /** 被分享文件/文件夹 id（仅个人空间） */
    private Long fileId;

    /** PERMANENT=永久 / DAYS=按天数 */
    private String validType;

    /** validType=DAYS 时有效期天数（上限 share.max-valid-days） */
    private Integer validDays;

    /** 是否设置提取码（requirePassword=true 时 accessPassword 必填） */
    private Boolean requirePassword;

    private String accessPassword;

    /** 下载策略：true=允许下载（maxDownload 可选）/ false=禁止下载（只能预览） */
    private Boolean allowDownload;

    /** 下载次数限制，0=不限（仅 allowDownload=true 时有效） */
    private Integer maxDownload;

    /** 允许转存 */
    private Boolean allowSave;
}
