package com.cloud.backend.service.system;

import com.cloud.backend.dto.meta.MetaOptionsResponse;

/**
 * 字典服务 —— 组装前端管理后台所需的业务枚举选项。
 * 枚举来自 Java Enum，运行时不变，前端登录后拉取一次即可。
 */
public interface MetaService {

    MetaOptionsResponse getOptions();
}
