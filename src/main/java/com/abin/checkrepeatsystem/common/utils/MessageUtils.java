package com.abin.checkrepeatsystem.common.utils;

import com.abin.checkrepeatsystem.pojo.entity.InstantMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 消息相关工具方法，供 StudentMessageServiceImpl 和 TeacherMessageServiceImpl 共享
 */
public class MessageUtils {

    private MessageUtils() {}

    /**
     * 生成聊天记录导出内容（TXT或HTML格式）
     */
    public static String generateChatExportContent(List<InstantMessage> messages, String format) {
        StringBuilder content = new StringBuilder();

        if ("txt".equalsIgnoreCase(format)) {
            content.append("聊天记录导出\n");
            content.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            content.append("========================================\n\n");

            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ?
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ?
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                content.append("[").append(time).append("] ").append(senderName).append(":\n");
                content.append(message.getContent()).append("\n\n");
            }
        } else {
            content.append("<html><head><meta charset=UTF-8><title>聊天记录</title></head><body>");
            content.append("<h1>聊天记录导出</h1>");
            content.append("<p>导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>");
            content.append("<hr>");

            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ?
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ?
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                content.append("<div style=margin: 10px 0; padding: 10px; border: 1px solid #ccc;>");
                content.append("<strong>").append(time).append(" ").append(senderName).append(":</strong><br>");
                content.append(message.getContent());
                content.append("</div>");
            }

            content.append("</body></html>");
        }

        return content.toString();
    }
}
