package com.cloud.backend.dto.share;

import lombok.Data;

import java.util.List;

/**
 * 访客批量下载/转存请求 —— 均为分享快照节点 id 列表。
 *
 * 修改指引：
 * - 【统一】修改 snapshotIds     → List&lt;Long&gt; snapshotIds；分享快照节点 id 列表（t_share_file 的 id），
 *                         对应 POST /api/shares/access/{token}/batch-download 与 save 入参；
 *                         注意传的是快照 id 而非原文件 id（防原文件 id 越权枚举）；改后需同步 t_share_file 快照校验与前端勾选传参
 * - 【统一】空列表               → 服务端按空处理（批量下载空清单会打包空结果），前端需保证至少勾选一个节点；改后需同步前端勾选校验与服务端空清单处理
 */
@Data
public class ShareAccessRequest {

    private List<Long> snapshotIds;
}
