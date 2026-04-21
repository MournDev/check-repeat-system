package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.common.websocket.handler.CheckProgressWebSocketHandler;
import com.abin.checkrepeatsystem.common.websocket.handler.NativeWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 原生 WebSocket 配置类
 * 配置原生 WebSocket 连接，支持实时消息推送和查重进度监控
 */
@Configuration
@EnableWebSocket
public class NativeWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private JwtUtils jwtUtils;

    @Bean
    public NativeWebSocketHandler nativeWebSocketHandler() {
        return new NativeWebSocketHandler(jwtUtils);
    }

    @Bean
    public CheckProgressWebSocketHandler checkProgressWebSocketHandler() {
        return new CheckProgressWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册原生 WebSocket 处理器，处理路径为 /ws/messages/{userId}
        registry.addHandler(nativeWebSocketHandler(), "/ws/messages/**")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000");
        
        // 注册查重进度 WebSocket 处理器，处理路径为 /ws/check-progress/{taskId}
        registry.addHandler(checkProgressWebSocketHandler(), "/ws/check-progress/**")
                .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000");
    }
}
