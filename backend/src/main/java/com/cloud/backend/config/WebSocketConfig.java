package com.cloud.backend.config;

import com.cloud.backend.websocket.ProgressWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 —— 注册统一进度通道 /ws/progress。
 * setAllowedOrigins("*")：允许前端开发服务器（localhost:5173）跨源连接。
 *
 * 修改指引：
 * - 【习惯】修改通道路径              → registerWebSocketHandlers() 中 "/ws/progress"；改动后需同步前端连接地址
 * - 【习惯】修改允许跨源来源           → setAllowedOrigins(...)；放开为 "*" 时任意来源可连接，收紧可限定前端域名；
 *                               改动后影响跨源 WebSocket 连接是否被拒绝
 * - 【习惯】新增/替换处理器           → addHandler(progressWebSocketHandler, ...)；改动后影响进度推送的消费端
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ProgressWebSocketHandler progressWebSocketHandler;

    public WebSocketConfig(ProgressWebSocketHandler progressWebSocketHandler) {
        this.progressWebSocketHandler = progressWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(progressWebSocketHandler, "/ws/progress").setAllowedOrigins("*");
    }
}
