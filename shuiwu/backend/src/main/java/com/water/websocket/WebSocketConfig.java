package com.water.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WaterWebSocketHandler waterWebSocketHandler;
    private final WebSocketInterceptor webSocketInterceptor;

    public WebSocketConfig(WaterWebSocketHandler waterWebSocketHandler,
                           WebSocketInterceptor webSocketInterceptor) {
        this.waterWebSocketHandler = waterWebSocketHandler;
        this.webSocketInterceptor = webSocketInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(waterWebSocketHandler, "/ws")
                .addInterceptors(webSocketInterceptor)
                .setAllowedOrigins("*");
    }
}