package com.abin.checkrepeatsystem.common.websocket.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.abin.checkrepeatsystem.common.utils.JwtUtils;

/**
 * 原生 WebSocket 处理器
 * 处理原生 WebSocket 连接，支持消息推送
 */
public class NativeWebSocketHandler extends TextWebSocketHandler {

    // 存储用户会话映射
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 存储会话认证状态
    private static final Map<WebSocketSession, String> sessionAuthStatus = new ConcurrentHashMap<>();

    // 存储会话对应的用户ID
    private static final Map<WebSocketSession, String> sessionUserIdMap = new ConcurrentHashMap<>();

    private JwtUtils jwtUtils;

    public NativeWebSocketHandler() {
    }

    public NativeWebSocketHandler(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        java.net.URI uri = session.getUri();
        String path = uri.getPath();
        // 连接建立时，先不认证，等待客户端发送认证消息
        sessionAuthStatus.put(session, "PENDING");
        System.out.println("✅ WebSocket 连接建立，等待认证: " + path);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息
        String payload = message.getPayload();

        try {
            // 解析消息
            com.alibaba.fastjson.JSONObject jsonMessage = com.alibaba.fastjson.JSON.parseObject(payload);
            String type = jsonMessage.getString("type");

            // 处理认证消息
            if ("AUTH".equals(type)) {
                handleAuthMessage(session, jsonMessage);
                return;
            }

            // 检查认证状态
            if (!"AUTHENTICATED".equals(sessionAuthStatus.get(session))) {
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 获取会话对应的用户ID
            String userId = sessionUserIdMap.get(session);

            if (userId != null) {
                // 根据消息类型进行不同的处理
                switch (type) {
                    case "CHAT":
                        // 处理聊天消息
                        handleChatMessage(jsonMessage, userId);
                        break;
                    case "TYPING":
                        // 处理输入状态消息
                        handleTypingMessage(jsonMessage, userId);
                        break;
                    case "READ":
                        // 处理已读状态消息
                        handleReadMessage(jsonMessage, userId);
                        break;
                    default:
                        // 未知消息类型
                }
            }
        } catch (Exception e) {
            System.err.println("处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理认证消息
     */
    private void handleAuthMessage(WebSocketSession session, com.alibaba.fastjson.JSONObject message) {
        try {
            String token = message.getString("token");
            String userId = message.getString("userId");

            // 验证token
            if (token == null || token.isEmpty()) {
                sessionAuthStatus.put(session, "FAILED");
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 验证token格式
            if (!token.contains(".") || token.split("\\.").length != 3) {
                sessionAuthStatus.put(session, "FAILED");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // 验证token签名和用户身份
            try {
                Long tokenUserId = jwtUtils.extractUserId(token);
                if (tokenUserId == null || !tokenUserId.toString().equals(userId)) {
                    sessionAuthStatus.put(session, "FAILED");
                    session.close(CloseStatus.POLICY_VIOLATION);
                    return;
                }

                // 认证成功
                sessionAuthStatus.put(session, "AUTHENTICATED");
                sessionUserIdMap.put(session, userId);
                userSessions.put(userId, session);

                System.out.println("✅ WebSocket 认证成功: " + userId);

                // 发送认证成功消息
                com.alibaba.fastjson.JSONObject response = new com.alibaba.fastjson.JSONObject();
                response.put("type", "AUTH_SUCCESS");
                response.put("message", "认证成功");
                session.sendMessage(new TextMessage(response.toJSONString()));

            } catch (Exception e) {
                System.err.println("❌ WebSocket 认证失败: " + e.getMessage());
                sessionAuthStatus.put(session, "FAILED");
                session.close(CloseStatus.POLICY_VIOLATION);
            }

        } catch (Exception e) {
            System.err.println("❌ 处理认证消息失败: " + e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ex) {
                // 忽略
            }
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(com.alibaba.fastjson.JSONObject message, String senderId) {
        // 处理聊天消息
        String receiverId = message.getString("receiverId");
        String conversationId = message.getString("conversationId");
        String content = message.getString("content");
        String senderName = message.getString("senderName");

        if (receiverId != null) {
            // 构建推送消息，包装成前端期望的格式
            com.alibaba.fastjson.JSONObject pushMessage = new com.alibaba.fastjson.JSONObject();
            pushMessage.put("type", "NEW_MESSAGE");

            com.alibaba.fastjson.JSONObject messageContent = new com.alibaba.fastjson.JSONObject();
            messageContent.put("conversationId", conversationId);
            messageContent.put("senderId", senderId);
            messageContent.put("senderName", senderName);
            messageContent.put("content", content);
            messageContent.put("sendTime", System.currentTimeMillis());

            pushMessage.put("content", messageContent);

            // 发送给指定用户
            sendMessageToUser(receiverId, pushMessage.toJSONString());
        }

        // System.out.println("聊天消息处理完成 - 发送者: " + senderId + ", 接收者: " + receiverId); // 移除调试输出
    }

    /**
     * 处理输入状态消息
     */
    private void handleTypingMessage(com.alibaba.fastjson.JSONObject message, String senderId) {
        // 通知对方用户正在输入
        String receiverId = message.getString("receiverId");
        if (receiverId != null) {
            sendMessageToUser(receiverId, message.toJSONString());
        }
    }

    /**
     * 处理已读状态消息
     */
    private void handleReadMessage(com.alibaba.fastjson.JSONObject message, String senderId) {
        // 更新消息已读状态
        String conversationId = message.getString("conversationId");
        // System.out.println("处理已读状态消息 - 会话ID: " + conversationId); // 移除调试输出
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 获取会话对应的用户ID
        String userId = sessionUserIdMap.get(session);

        System.out.println("WebSocket 连接关闭: 用户=" + userId + ", 状态=" + status);

        if (userId != null) {
            userSessions.remove(userId);
            sessionUserIdMap.remove(session);
            sessionAuthStatus.remove(session);
            System.out.println("WebSocket 用户会话移除: 用户 " + userId + ", 当前在线用户数: " + userSessions.size());
        } else {
            sessionAuthStatus.remove(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket 传输错误: " + exception.getMessage());
    }

    /**
     * 从路径中提取用户ID
     * @param path WebSocket 连接路径
     * @return 用户ID
     */
    private String extractUserId(String path) {
        if (path == null) {
            return null;
        }
        // 路径格式: /check/ws/messages/{userId} 或 /ws/messages/{userId}
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            // 检查是否包含/check前缀
            if (parts.length >= 5 && "check".equals(parts[1])) {
                return parts[4];
            } else {
                return parts[3];
            }
        }
        return null;
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                System.err.println("发送消息失败: " + e.getMessage());
            }
        }
    }

    /**
     * 获取当前连接的用户数
     * @return 连接用户数
     */
    public static int getConnectedUserCount() {
        return userSessions.size();
    }
}
