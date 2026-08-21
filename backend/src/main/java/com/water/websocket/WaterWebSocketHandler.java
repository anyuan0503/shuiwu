package com.water.websocket;

import com.water.common.constants.CommonConstants;
import com.water.service.MonitorService;
import com.water.vo.RealtimeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 消息处理器
 */
@Slf4j
@Component
public class WaterWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final MonitorService monitorService;

    public WaterWebSocketHandler(WebSocketSessionManager sessionManager, MonitorService monitorService) {
        this.sessionManager = sessionManager;
        this.monitorService = monitorService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String name = session.getAttributes().containsKey("username")
                ? String.valueOf(session.getAttributes().get("username")) : session.getId();
        sessionManager.addSession(session.getId(), session);
        log.info("WebSocket 连接建立: {} 当前在线 {}", name, sessionManager.count());
        // 连接建立后立即推送当前实时数据
        List<RealtimeVO> realtime = monitorService.getRealtimeList();
        sessionManager.sendTo(session.getId(), CommonConstants.WS_TYPE_REALTIME, realtime);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload) || "heartbeat".equalsIgnoreCase(payload)) {
            sessionManager.sendTo(session.getId(), CommonConstants.WS_TYPE_HEARTBEAT, "pong");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session.getId());
        log.info("WebSocket 连接关闭，当前在线 {}", sessionManager.count());
    }
}