package com.cloud.backend.dto.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 字典选项 —— 业务枚举组的单个选项。
 * 只含业务语义（value + label），颜色/图标等 UI 样式归前端维护。
 *
 * 修改指引：
 * - 【统一】修改 value           → String value；业务枚举的存储值/提交值，前端表单提交与后端反查用，改动需与枚举值保持一致；改后需同步枚举存储值与前端表单提交/后端反查
 * - 【习惯】修改 label           → String label；展示文案，前端下拉/标签直接显示；改动为文案调整，不涉及数据契约
 * - 【习惯】新增字段（如颜色/图标）→ 违背"UI 样式归前端维护"的设计，建议放前端映射而不进接口
 */
@Data
@AllArgsConstructor
public class OptionItem {

    private String value;
    private String label;
}
