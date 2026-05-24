package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.common.websocket.handler.CheckProgressWebSocketHandler;
import com.abin.checkrepeatsystem.common.websocket.handler.NativeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;


/**
 * 原生 WebSocket 配置类
 * 配置原生 WebSocket 连接，支持实时消息推送和查重进度监控
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSocket
public class NativeWebSocketConfig implements WebSocketConfigurer {

    private final JwtUtils jwtUtils;

    @Value("${websocket.allowed-origins:}")
    private String allowedOriginsConfig;

    @Bean
    public NativeWebSocketHandler nativeWebSocketHandler() {
        return new NativeWebSocketHandler(jwtUtils);
    }

    @Bean
    public CheckProgressWebSocketHandler checkProgressWebSocketHandler() {
        return new CheckProgressWebSocketHandler();
    }

    private String[] getAllowedOrigins() {
        return Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = getAllowedOrigins();
        // 注册原生 WebSocket 处理器，处理路径为 /ws/messages/{userId}
        registry.addHandler(nativeWebSocketHandler(), "/ws/messages/**")
                .setAllowedOrigins(origins);

        // 注册查重进度 WebSocket 处理器，处理路径为 /ws/check-progress/{taskId}
        registry.addHandler(checkProgressWebSocketHandler(), "/ws/check-progress/**")
                .setAllowedOrigins(origins);
    }
}
