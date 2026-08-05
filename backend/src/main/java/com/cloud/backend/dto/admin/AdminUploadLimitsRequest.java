package com.cloud.backend.dto.admin;

import lombok.Data;

/**
 * 上传限制更新请求（null 字段保持原值）。
 *
 * 修改指引：
 * - 【习惯】修改 maxSizeUser/maxSizeVip → Long 单文件上传大小上限（单位字节）；改动需同步上传校验逻辑与前端上传提示
 * - 【习惯】修改 maxConcurrentUser/maxConcurrentVip → Integer 并发上传数上限；改动需同步并发校验逻辑与前端上传队列
 * - 【习惯】修改 null 语义         → null 字段保持原值（不更新）；改动需同步 service 的空值判断，否则会影响未传字段
 * - 【习惯】新增限制项            → 新增字段并同步上传校验逻辑与前端，否则该限制不生效
 */
@Data
public class AdminUploadLimitsRequest {

    private Long maxSizeUser;
    private Long maxSizeVip;
    private Integer maxConcurrentUser;
    private Integer maxConcurrentVip;
}
