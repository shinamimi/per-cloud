package com.cloud.backend.dto.share;

import lombok.Data;

import java.util.List;

/**
 * 访客批量下载/转存请求 —— 均为分享快照节点 id 列表。
 */
@Data
public class ShareAccessRequest {

    private List<Long> snapshotIds;
}
