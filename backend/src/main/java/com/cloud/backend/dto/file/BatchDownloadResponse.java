package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 批量打包下载响应（异步任务状态）。
 * status: PENDING / PACKING / DONE / FAILED
 * url 在 DONE 时有效（presigned，可直接下载）。
 */
@Data
public class BatchDownloadResponse {

    private String taskId;
    private String status;
    private Integer total;
    private Integer done;
    private String url;

    public static BatchDownloadResponse of(String taskId, String status, Integer total, Integer done, String url) {
        BatchDownloadResponse response = new BatchDownloadResponse();
        response.setTaskId(taskId);
        response.setStatus(status);
        response.setTotal(total);
        response.setDone(done);
        response.setUrl(url);
        return response;
    }
}
