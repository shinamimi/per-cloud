package com.cloud.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * 用途：自动生成 API 文档，开发阶段通过 /swagger-ui.html 在线调试接口。
 * 不使用 Springfox（已停止维护），改为 SpringDoc 组件。
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Backend API")
                        .description("个人云盘后端 API 文档")
                        .version("1.0.0")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}