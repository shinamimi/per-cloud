package com.cloud.backend.entity;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享记录实体 —— 对应数据库 t_share 表。
 *
 * 设计思路：
 * shareToken 是分享链接的唯一标识（10 位去混淆短码 + 生成时查重）。
 * accessPassword 可选密码保护（为空则公开访问）。
 * maxDownload / downloadCount 控制下载次数限制（0=不限；全局共享累计，达限状态变 EXHAUSTED）。
 * expireTime 控制过期时间（NULL=永久分享）。
 * allowDownload / allowSave 下载策略与转存开关。
 * isDir = 1 表示目录分享（创建时锁定快照 t_share_file）。
 *
 * 修改指引：
 * - 【习惯】修改 id / userId / fileId → Long id（t_share.id 主键）/ Long userId（user_id 分享者）/ Long fileId（file_id 分享的文件）；
 *                            fileId 关联 t_file，改它需同步下载/预览校验逻辑
 * - 【习惯】修改 isDir             → Integer isDir；对应 t_share.is_dir（TINYINT），1=目录分享（创建时锁定快照 t_share_file）
 *                            0=单文件分享；改它影响快照生成逻辑
 * - 【习惯】修改 shareToken        → String shareToken；对应 t_share.share_token（唯一索引），分享链接唯一标识
 *                            （10 位去混淆短码 + 生成时查重），改生成规则需同步访问链接与 DDL
 * - 【习惯】修改 accessPassword    → String accessPassword；对应 t_share.access_password，空=公开访问，改校验逻辑在分享访问处
 * - 【习惯】修改 status            → ShareStatus status；对应 t_share.status（TINYINT），NORMAL=0/EXPIRED=1/CANCELED=2/EXHAUSTED=3
 *                            （见 enums/ShareStatus.java，按 ordinal 存库），改枚举见 ShareStatus 修改指引
 * - 【习惯】修改 expireTime        → LocalDateTime expireTime；对应 t_share.expire_time，NULL=永久分享，过期后状态置 EXPIRED，
 *                            改它影响过期判定
 * - 【习惯】修改 maxDownload / downloadCount → Integer maxDownload（t_share.max_download，0=不限）/ Integer downloadCount
 *                            （download_count，全局共享累计）；达上限状态置 EXHAUSTED，改上限规则需联动下载拦截
 * - 【习惯】修改 allowDownload / allowSave → Integer allowDownload（allow_download，1=允许下载 0=禁止，只能在线预览）/
 *                            Integer allowSave（allow_save，1=允许转存 0=禁止）；改它们影响访客下载/转存入口
 * - 【习惯】修改 createdAt / updatedAt → LocalDateTime；自动维护，无业务联动
 */
@Data
public class Share {

    private Long id;
    private Long userId;
    private Long fileId;
    /** 1=目录分享（快照锁定） 0=单文件分享 */
    private Integer isDir;
    private String shareToken;
    private String accessPassword;
    private ShareStatus status;
    private LocalDateTime expireTime;
    private Integer maxDownload;
    private Integer downloadCount;
    /** 1=允许下载 0=禁止下载（只能在线预览） */
    private Integer allowDownload;
    /** 1=允许转存 0=禁止转存 */
    private Integer allowSave;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}