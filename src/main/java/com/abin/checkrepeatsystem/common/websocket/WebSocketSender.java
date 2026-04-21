package com.abin.checkrepeatsystem.common.websocket;

import com.abin.checkrepeatsystem.common.websocket.handler.NativeWebSocketHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息发送器
 * 使用原生 WebSocket 实现
 */
@Component
@Slf4j
public class WebSocketSender {

    /**
     * 发送点对点消息
     * @param userId 接收用户ID
     * @param message 消息内容
     */
    public void sendToUser(Long userId, Object message) {
        try {
            sendNativeWebSocketMessage(userId, message);
        } catch (Exception e) {
            log.error("发送点对点消息失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 通过原生 WebSocket 发送消息
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void sendNativeWebSocketMessage(Long userId, Object message) {
        try {
            String messageJson = JSON.toJSONString(message);
            NativeWebSocketHandler.sendMessageToUser(userId.toString(), messageJson);
            log.debug("通过原生 WebSocket 发送消息成功 - 用户ID: {}, 消息: {}", 
                     userId, messageJson);
        } catch (Exception e) {
            log.error("通过原生 WebSocket 发送消息失败 - 用户ID: {}", userId, e);
        }
    }
    
    /**
     * 获取在线用户数量
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        // 统计原生 WebSocket 的在线用户数
        return NativeWebSocketHandler.getConnectedUserCount();
    }
}