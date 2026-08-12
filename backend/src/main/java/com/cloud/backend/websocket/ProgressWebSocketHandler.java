package com.cloud.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 统一进度通道 /ws/progress。
 *
 * 设计思路：
 * 上传分片进度、批量打包进度均通过此通道推送（单实例广播；多实例时用 Redis Pub/Sub 扩展，
 * 见 file-module.md 11 节扩展预留）。消息格式：{type: "upload"|"download", ...业务字段}。
 *
 * 修改指引：
 * - 【统一】修改消息格式            → broadcast 中的 {type, ...payload}；改动需同步前端解析与 file-module.md 消息格式约定；
 *                             改后需同步前端解析与 file-module.md 消息格式约定
 * - 【习惯】修改广播范围/定向推送   → sessions 集合与 broadcast 循环；当前为单实例全量广播，多实例需改 Redis Pub/Sub
 * - 【习惯】修改连接生命周期处理    → afterConnectionEstablished / afterConnectionClosed / handleTransportError；
 *                             当前会话存于内存 CopyOnWriteArraySet，服务重启即失效
 * - 【统一】新增业务推送类型        → 调用方以 type 区分（upload/download）；新增类型需在前端注册对应处理；
 *                             改后需同步调用方 type 取值与前端注册对应处理
 */
@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProgressWebSocketHandler.class);

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket closed: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        log.warn("WebSocket transport error: {}", session.getId(), exception);
    }

    /** 广播进度消息 */
    public void broadcast(String type, Map<String, Object> payload) {
        Map<String, Object> message = new java.util.HashMap<>();
        message.put("type", type);
        message.putAll(payload);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            log.warn("WebSocket message serialization failed", e);
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("WebSocket send failed: {}", session.getId(), e);
                }
            }
        }
    }
}
