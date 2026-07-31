package com.cloud.backend.dto.meta;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 字典接口响应体 —— GET /api/meta/options。
 * 按组（group）组织，新增枚举组无需修改接口定义。
 */
@Data
public class MetaOptionsResponse {

    private Map<String, List<OptionItem>> groups;

    public MetaOptionsResponse(Map<String, List<OptionItem>> groups) {
        this.groups = groups;
    }
}
