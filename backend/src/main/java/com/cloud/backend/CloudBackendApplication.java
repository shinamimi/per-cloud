package com.cloud.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cloud 云盘 —— 后端入口。
 *
 * @SpringBootApplication：组合了 @Configuration、@EnableAutoConfiguration、@ComponentScan
 * @ConfigurationPropertiesScan：自动扫描 @ConfigurationProperties 注解的类并注册为 Bean，
 *   省去了在每个 Properties 类上加 @Component 或在配置类中挨个 @EnableConfigurationProperties 的麻烦。
 * @EnableScheduling：启用定时任务（回收站 30 天清理、打包产物清理等）
 *
 * 修改指引：
 * - 【习惯】调整组件扫描范围        → @SpringBootApplication 的 scanBasePackages；当前默认扫描 com.cloud.backend 包
 * - 【习惯】新增配置属性类          → 无需在此类改动，@ConfigurationPropertiesScan 已自动扫描注册 @ConfigurationProperties Bean
 * - 【习惯】启停定时任务            → @EnableScheduling；当前已启用，移除后配置类中所有 @Scheduled 任务停止执行
 * - 【习惯】新增全局装配            → 建议新建 config 包配置类而非在此类堆积；本类只做启动装配
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBackendApplication.class, args);
    }

}