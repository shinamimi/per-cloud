package com.cloud.backend.dto.share;

import com.cloud.backend.enums.ShareStatus;
import lombok.Data;

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
