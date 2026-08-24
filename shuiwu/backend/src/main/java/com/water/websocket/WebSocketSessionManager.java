package com.water.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器，提供广播能力
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addSession(String key, WebSocketSession session) {
        sessions.put(key, session);
    }

    public void removeSession(String key) {
        sessions.remove(key);
    }

    public int count() {
        return sessions.size();
    }

    /**
     * 向所有连接广播消息 {type, data}
     */
    public void broadcast(String type, Object data) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage message = buildMessage(type, data);
        if (message == null) {
            return;
        }
        sessions.forEach((k, session) -> {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (Exception e) {
                    log.warn("WebSocket 推送失败: {}", e.getMessage());
                }
            }
        });
    }

    public void sendTo(String key, String type, Object data) {
        WebSocketSession session = sessions.get(key);
        if (session == null || !session.isOpen()) {
            return;
        }
        TextMessage message = buildMessage(type, data);
        if (message == null) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (Exception e) {
            log.warn("WebSocket 单发失败: {}", e.getMessage());
        }
    }

    private TextMessage buildMessage(String type, Object data) {
        try {
            Map<String, Object> map = Map.of("type", type, "data", data);
            return new TextMessage(objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.error("消息序列化失败", e);
            return null;
        }
    }
}