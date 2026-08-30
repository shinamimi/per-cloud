package com.cloud.backend.dto.admin;

import lombok.Data;

@Data
public class AdminShareDownloadRequest {

    /** true=允许下载 false=禁止下载 */
    private boolean allowDownload;
}
