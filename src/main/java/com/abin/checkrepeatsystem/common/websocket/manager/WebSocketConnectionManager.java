package com.abin.checkrepeatsystem.common.websocket.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket连接状态管理器
 */
@Component
@Slf4j
public class WebSocketConnectionManager {

    // 存储用户连接状态 <userId, sessionId>
    private final ConcurrentHashMap<Long, String> userConnections = new ConcurrentHashMap<>();
    
    // 在线用户计数器
    private final AtomicInteger onlineUserCount = new AtomicInteger(0);

    /**
     * 用户连接
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void userConnect(Long userId, String sessionId) {
        String previousSession = userConnections.put(userId, sessionId);
        if (previousSession == null) {
            // 新用户连接，计数+1
            onlineUserCount.incrementAndGet();
        } else if (!previousSession.equals(sessionId)) {
            // 同一用户重连（sessionId 变了），计数不变
            log.info("用户重连 - 用户ID: {}, 旧会话: {}, 新会话: {}", userId, previousSession, sessionId);
        }
        log.info("用户连接 - 用户ID: {}, 会话ID: {}, 当前在线用户数: {}",
                userId, sessionId, onlineUserCount.get());
    }

    /**
     * 用户断开连接
     * @param userId 用户ID
     */
    public void userDisconnect(Long userId) {
        if (userConnections.remove(userId) != null) {
            int newCount = onlineUserCount.decrementAndGet();
            // 防止计数器出现负数
            if (newCount < 0) {
                onlineUserCount.set(0);
                log.warn("WebSocket在线用户计数器异常（负数），已重置为0");
            }
            log.info("用户断开连接 - 用户ID: {}, 当前在线用户数: {}",
                    userId, onlineUserCount.get());
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        return userConnections.containsKey(userId);
    }

    /**
     * 获取用户会话ID
     * @param userId 用户ID
     * @return 会话ID
     */
    public String getUserSessionId(Long userId) {
        return userConnections.get(userId);
    }

    /**
     * 获取在线用户数量（基于计数器，可能存在偏差）
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return onlineUserCount.get();
    }

    /**
     * 获取实际在线用户数量（基于连接表，精确值）
     * @return 实际在线用户数
     */
    public int getActualOnlineCount() {
        return userConnections.size();
    }

    /**
     * 获取所有在线用户ID
     * @return 在线用户ID列表
     */
    public java.util.Set<Long> getOnlineUserIds() {
        return userConnections.keySet();
    }

    /**
     * 清理所有连接（系统关闭时调用）
     */
    public void clearAllConnections() {
        int count = userConnections.size();
        userConnections.clear();
        onlineUserCount.set(0);
        log.info("清理所有WebSocket连接 - 清理数量: {}", count);
    }
}