package com.abin.checkrepeatsystem.common.websocket.handler;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查重进度 WebSocket 处理器
 * 处理查重进度的 WebSocket 连接，支持实时进度推送
 */
public class CheckProgressWebSocketHandler extends TextWebSocketHandler {

    // 存储任务ID与会话的映射
    private static final Map<String, WebSocketSession> taskSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从路径中获取任务ID
        String path = session.getUri().getPath();
        String taskId = extractTaskId(path);
        
        if (taskId != null) {
            taskSessions.put(taskId, session);
            // System.out.println("查重进度 WebSocket 连接建立: 任务 " + taskId); // 移除调试输出
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息
        // System.out.println("收到查重进度消息: " + message.getPayload()); // 移除调试输出
        // 可以根据需要处理消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从路径中获取任务ID
        String path = session.getUri().getPath();
        String taskId = extractTaskId(path);
        
        if (taskId != null) {
            taskSessions.remove(taskId);
            // System.out.println("查重进度 WebSocket 连接关闭: 任务 " + taskId); // 移除调试输出
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // System.err.println("查重进度 WebSocket 传输错误: " + exception.getMessage()); // 移除调试输出
    }

    /**
     * 从路径中提取任务ID
     * @param path WebSocket 连接路径
     * @return 任务ID
     */
    private String extractTaskId(String path) {
        if (path == null) {
            return null;
        }
        // 路径格式: /ws/check-progress/{taskId}
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }

    /**
     * 向指定任务发送进度消息
     * @param taskId 任务ID
     * @param message 消息内容
     */
    public static void sendProgressMessage(String taskId, String message) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                // System.err.println("发送查重进度消息失败: " + e.getMessage()); // 移除调试输出
            }
        }
    }

    /**
     * 获取当前连接的任务数
     * @return 连接任务数
     */
    public static int getConnectedTaskCount() {
        return taskSessions.size();
    }
}
