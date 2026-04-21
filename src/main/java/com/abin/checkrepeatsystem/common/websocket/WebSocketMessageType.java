package com.abin.checkrepeatsystem.common.websocket;

/**
 * WebSocket 消息类型枚举
 */
public enum WebSocketMessageType {
    // 新消息
    NEW_MESSAGE("NEW_MESSAGE"),
    // 消息已读
    MESSAGE_READ("MESSAGE_READ"),
    // 对方正在输入
    TYPING("TYPING"),
    // 会话更新
    CONVERSATION_UPDATE("CONVERSATION_UPDATE"),
    // 系统通知
    SYSTEM_NOTIFICATION("SYSTEM_NOTIFICATION");

    private final String value;

    WebSocketMessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}