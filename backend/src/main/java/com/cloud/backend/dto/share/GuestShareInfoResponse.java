package com.cloud.backend.dto.share;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

/**
 * 访客获取分享信息 —— GET /api/shares/access/{token}。
 * 未验证提取码时也返回（requirePassword=true 供前端弹出密码框），文件树/下载需验证。
 *
 * 修改指引：
 * - 【统一】修改 shareToken      → String shareToken；分享 token（10 位去混淆短码，唯一标识），前端据此拼装分享链接；
 *                         改动生成规则需同步 ShareTokenGenerator，历史链接会失效；改后需同步 ShareTokenGenerator 与历史链接兼容
 * - 【习惯】修改 isDir           → Boolean isDir；根节点是否目录
 * - 【习惯】修改 name            → String name；根节点名（单文件=文件名，目录=目录名）
 * - 【习惯】修改 ownerName       → String ownerName；分享者昵称
 * - 【统一】修改 status          → ShareStatus status；自定义枚举（enums/ShareStatus.java）：
 *                         NORMAL=0 生效中 / EXPIRED=1 已过期 / CANCELED=2 已取消 / EXHAUSTED=3 下载已达上限；
 *                         前端据此展示"已过期/已取消/已达上限"等；改后需同步 enums/ShareStatus.java 与前端状态展示
 * - 【统一】修改 requirePassword → Boolean requirePassword；true 时前端弹提取码框；未验证时本接口也返回，文件树/下载需先验证；改后需同步前端访问流程与服务端提取码校验
 * - 【统一】修改 allowDownload / allowSave → 是否允许下载/转存，前端据此隐藏下载/转存按钮；改后需同步分享访问校验与前端按钮显隐
 * - 【统一】修改 maxDownload / downloadCount → 下载次数上限/已下载次数（maxDownload=0 表示不限）；
 *                         downloadCount 达到上限时服务端置 EXHAUSTED；改后需同步下载计数与 EXHAUSTED 状态流转
 * - 【习惯】修改 fileCount       → Integer fileCount；文件总数（仅目录分享展示），单文件分享为 null
 */
@Data
public class GuestShareInfoResponse {

    private String shareToken;
    private Boolean isDir;
    /** 根节点名（单文件=文件名，目录=目录名） */
    private String name;
    /** 分享者昵称 */
    private String ownerName;
    private ShareStatus status;
    private Boolean requirePassword;
    private Boolean allowDownload;
    private Boolean allowSave;
    private Integer maxDownload;
    private Integer downloadCount;
    /** 文件总数（仅目录分享展示） */
    private Integer fileCount;
}
