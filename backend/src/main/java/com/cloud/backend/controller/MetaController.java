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
 *
 * 修改指引：
 * - 【习惯】拉取字典           → GET /api/meta/options，调 metaService.getOptions()；需登录（SecurityConfig /api/** authenticated），
 *                        改动影响前端字典组结构，新增枚举组在 MetaService 内组装即可，无需改动本接口
 * - 【习惯】新增/修改接口       → 在 @RequestMapping("/api/meta") 下新增；需登录，若改为公开接口须在 SecurityConfig 放行并同步前端 API 层
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
