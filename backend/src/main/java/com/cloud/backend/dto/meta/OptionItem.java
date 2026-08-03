package com.cloud.backend.dto.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 字典选项 —— 业务枚举组的单个选项。
 * 只含业务语义（value + label），颜色/图标等 UI 样式归前端维护。
 */
@Data
@AllArgsConstructor
public class OptionItem {

    private String value;
    private String label;
}
