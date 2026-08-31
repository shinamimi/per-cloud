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
