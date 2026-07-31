package com.cloud.backend.controller;

import com.cloud.backend.dto.Result;
import com.cloud.backend.dto.meta.MetaOptionsResponse;
import com.cloud.backend.service.system.MetaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典接口 —— 管理后台登录后拉取一次，前端统一通过 groups.xxx 获取业务枚举。
 * 新增枚举组无需修改接口定义。
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    @GetMapping("/options")
    public Result<MetaOptionsResponse> getOptions() {
        return Result.success(metaService.getOptions());
    }
}
