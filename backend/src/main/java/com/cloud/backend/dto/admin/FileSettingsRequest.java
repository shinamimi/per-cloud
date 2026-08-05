package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 文件管理配置更新请求（null 字段恢复配置默认值）。
 *
 * 修改指引：
 * - 【习惯】修改单位             → recycleBinDays/shareDefaultValidDays/shareMaxValidDays 单位均为天；改动需同步配置读写逻辑与前端
 * - 【习惯】修改 shareDefaultDownloadPolicy → String：ALLOW=允许下载（默认）/DENY=禁止下载；改动需同步分享默认策略逻辑与前端下拉
 * - 【习惯】修改 shareDefaultRequirePassword → Boolean 分享默认是否要求提取码；改动影响新建分享的默认行为
 * - 【习惯】修改 shareMaxCountPerFile → Integer 同一文件最大分享次数；改动需同步分享创建校验
 * - 【习惯】修改 null 语义         → null 字段恢复配置默认值；改动需同步 service 的空值判断，否则会影响未传字段
 */
@Data
public class FileSettingsRequest {

    /** 回收站保留天数 */
    private Integer recycleBinDays;

    /** 分享默认有效期（天） */
    private Integer shareDefaultValidDays;

    /** 分享最长有效期（天） */
    private Integer shareMaxValidDays;

    /** 同一文件最大分享次数 */
    private Integer shareMaxCountPerFile;

    /** 分享默认是否要求提取码 */
    private Boolean shareDefaultRequirePassword;

    /** 分享默认下载策略：ALLOW=允许下载（默认）/ DENY=禁止下载 */
    private String shareDefaultDownloadPolicy;
}
