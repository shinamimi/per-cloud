package com.cloud.backend.dto.admin;

import lombok.Data;

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
