package com.cloud.backend.dto.file;

import lombok.Data;

/**
 * 批量打包下载响应（异步任务状态）。
 * status: PENDING / PACKING / DONE / FAILED
 * url 在 DONE 时有效（presigned，可直接下载）。
 *
 * 修改指引：
 * - 【习惯】修改 taskId          → String taskId；异步任务唯一标识，前端轮询 GET /api/files/download/batch/{taskId}
 *                         查询进度；任务存于进程内存，服务重启后任务即不可查
 * - 【习惯】修改 status          → String status；取值 PENDING / PACKING / DONE / FAILED，前端据此切换轮询/展示结果；
 *                         新增状态需同步 DownloadService 打包流程与前端分支
 * - 【习惯】修改 total / done    → Integer total / Integer done；打包文件总数与已完成数（单位：个），前端做进度条；
 *                         done == total 只代表打包完成，不代表 url 已可用（DONE 才有效）
 * - 【习惯】修改 url             → String url；DONE 时有效的预签名 zip 下载地址，有效期受管理员"下载链接有效期"配置影响，
 *                         过期后需重新打包获取新地址
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
