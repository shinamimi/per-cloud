package com.cloud.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Cloud 云盘 —— 后端入口。
 *
 * @SpringBootApplication：组合了 @Configuration、@EnableAutoConfiguration、@ComponentScan
 * @ConfigurationPropertiesScan：自动扫描 @ConfigurationProperties 注解的类并注册为 Bean，
 *   省去了在每个 Properties 类上加 @Component 或在配置类中挨个 @EnableConfigurationProperties 的麻烦。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBackendApplication.class, args);
    }

}