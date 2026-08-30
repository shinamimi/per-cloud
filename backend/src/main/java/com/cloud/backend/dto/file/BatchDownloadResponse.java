package com.cloud.backend.dto.file;

import lombok.Data;

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
