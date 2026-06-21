package com.abin.checkrepeatsystem.common.websocket.handler;

import com.abin.checkrepeatsystem.common.service.TokenRevocationService;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    private final TokenRevocationService tokenRevocationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            String token = req.getParameter("token");

            if (token == null || token.isBlank()) {
                String authHeader = req.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || token.isBlank()) {
                log.warn("WebSocket握手失败: 缺少token");
                return false;
            }

            try {
                Long userId = jwtUtils.getUserIdFromToken(token);
                if (userId == null) {
                    log.warn("WebSocket握手失败: 无效token");
                    return false;
                }
                // 检查 token 是否在密码修改后被吊销
                java.util.Date issuedAt = jwtUtils.extractAllClaims(token).getIssuedAt();
                if (tokenRevocationService.isTokenRevoked(userId, issuedAt)) {
                    log.warn("WebSocket握手失败: token已被吊销（密码已修改），userId={}", userId);
                    return false;
                }
                attributes.put("userId", userId.toString());
                log.info("WebSocket握手成功: userId={}", userId);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket握手失败: token验证异常", e);
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
