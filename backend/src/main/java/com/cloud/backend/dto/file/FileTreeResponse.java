package com.cloud.backend.dto.file;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录树节点响应。
 *
 * 修改指引：
 * - 【统一】修改 id / name       → Long id / String name；节点 id 与名称，目录树导航/移动目标选择用；改名需同步前端目录树逻辑与 Service 组装
 * - 【习惯】修改 isDirectory     → Boolean isDirectory；是否目录，前端按此决定是否可展开/可作目标目录
 * - 【习惯】修改 children        → List&lt;FileTreeResponse&gt; children；子节点列表，默认空列表，前端按 children 递归渲染；
 *                         改动为懒加载时需同步接口契约（按需加载子节点）
 */
@Data
public class FileTreeResponse {

    private Long id;
    private String name;
    private Boolean isDirectory;
    private List<FileTreeResponse> children = new ArrayList<>();

    public static FileTreeResponse of(Long id, String name, boolean isDirectory) {
        FileTreeResponse node = new FileTreeResponse();
        node.setId(id);
        node.setName(name);
        node.setIsDirectory(isDirectory);
        return node;
    }
}
