package com.cloud.backend.websocket;

import com.cloud.backend.entity.User;
import com.cloud.backend.mapper.UserMapper;
import com.cloud.backend.utils.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一进度通道 /ws/progress。
 *
 * 设计思路：
 * 上传分片进度、批量打包进度均通过此通道推送（按用户隔离；多实例时用 Redis Pub/Sub 扩展，
 * 见 file-module.md 11 节扩展预留）。消息格式：{type: "upload"|"download", ...业务字段}。
 *
 * 修改指引：
 * - 【统一】修改消息格式            → broadcast/sendToUser 中的 {type, ...payload}；改动需同步前端解析与 file-module.md 消息格式约定；
 *                             改后需同步前端解析与 file-module.md 消息格式约定
 * - 【习惯】修改广播范围/定向推送   → userSessions 映射与 sendToUser/broadcast 循环；当前为单实例用户隔离，多实例需改 Redis Pub/Sub
 * - 【习惯】修改连接生命周期处理    → afterConnectionEstablished / afterConnectionClosed / handleTransportError；
 *                             当前会话存于内存 ConcurrentHashMap，服务重启即失效
 * - 【统一】新增业务推送类型        → 调用方以 type 区分（upload/download）；新增类型需在前端注册对应处理；
 *                             改后需同步调用方 type 取值与前端注册对应处理
 */
@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProgressWebSocketHandler.class);

    /** 按 userId 隔离的会话映射，替代全局广播 */
    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenUtil jwtTokenUtil;
    private final UserMapper userMapper;

    public ProgressWebSocketHandler(JwtTokenUtil jwtTokenUtil, UserMapper userMapper) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userMapper = userMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractTokenFromUri(session);
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            log.warn("WebSocket connection rejected: invalid token");
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session", e);
            }
            return;
        }

        String username = jwtTokenUtil.getUsernameFromToken(token);
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("WebSocket connection rejected: user not found");
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session", e);
            }
            return;
        }

        Long userId = user.getId();
        WebSocketSession oldSession = userSessions.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                log.warn("Failed to close old WebSocket session", e);
            }
        }
        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.values().removeIf(s -> s.equals(session));
        log.info("WebSocket closed: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        userSessions.values().removeIf(s -> s.equals(session));
        log.warn("WebSocket transport error: {}", session.getId(), exception);
    }

    /** 向指定用户发送进度消息 */
    public void sendToUser(Long userId, String type, Map<String, Object> payload) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }

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

        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("WebSocket send failed: userId={}", userId, e);
        }
    }

    /** 广播给所有连接用户（管理员通知等场景） */
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
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("WebSocket broadcast failed: {}", session.getId(), e);
                }
            }
        }
    }

    private String extractTokenFromUri(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null) return null;
            return UriComponentsBuilder.newInstance()
                    .query(query)
                    .build()
                    .getQueryParams()
                    .getFirst("token");
        } catch (Exception e) {
            return null;
        }
    }
}
