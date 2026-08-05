package com.cloud.backend.dto.meta;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 字典接口响应体 —— GET /api/meta/options。
 * 按组（group）组织，新增枚举组无需修改接口定义。
 *
 * 修改指引：
 * - 【习惯】修改 groups          → Map&lt;String, List&lt;OptionItem&gt;&gt; groups；键为枚举组名（如 role / userStatus），
 *                         前端统一通过 groups.xxx 获取业务枚举；新增枚举组在 MetaService 组装配即可，无需改接口定义
 * - 【习惯】修改 groups 的 key 名 → 影响前端所有取字典处，需前后端同步改动
 * - 【习惯】改响应结构           → 若改为固定字段集合，将失去"新增枚举组零改动"特性，前端取值方式也要改
 */
@Data
public class MetaOptionsResponse {

    private Map<String, List<OptionItem>> groups;

    public MetaOptionsResponse(Map<String, List<OptionItem>> groups) {
        this.groups = groups;
    }
}
