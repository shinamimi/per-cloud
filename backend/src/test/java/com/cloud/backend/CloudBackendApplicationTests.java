package com.cloud.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 启动上下文测试 —— 验证 Spring 容器能否正常加载。
 * 测试时会加载所有 Bean（数据源、Redis、MinIO 等），
 * 如果配置或依赖有问题会在此处报错。
 */
@SpringBootTest
class CloudBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}