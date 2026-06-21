package com.abin.checkrepeatsystem.student.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 *消息VO
 */
@Data
@Schema(description = "消息")
public class MessageVO {

    @Schema(description = "消息ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "发送者ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long senderId;

    @Schema(description = "发送者姓名")
    private String senderName;

    @Schema(description = "发送者角色")
    private String senderRole;

    @Schema(description = "发送者标识 (student/teacher/advisor)")
    private String sender;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private String sendTime;

    @Schema(description = "消息状态")
    private String status;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "附件列表")
    private List<MessageAttachmentVO> attachments;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "格式化后的时间（相对时间）")
    private String formattedTime;

    @Schema(description = "会话ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sessionId;

    @Schema(description = "会话ID（兼容前端）")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long conversationId;

    /**
     * 获取格式化的时间显示
     */
    public String getFormattedTime() {
        if (sendTime == null || sendTime.isEmpty()) {
            return "";
        }

        try {
            LocalDateTime sentTime = LocalDateTime.parse(sendTime);
            LocalDateTime now = LocalDateTime.now();
            long minutesBetween = java.time.Duration.between(sentTime, now).toMinutes();

            if (minutesBetween < 1) {
                return "刚刚";
            } else if (minutesBetween < 60) {
                return minutesBetween + "分钟前";
            } else if (minutesBetween < 1440) { // 24 小时内
                long hours = minutesBetween / 60;
                return hours + "小时前";
            } else if (sentTime.toLocalDate().equals(now.toLocalDate())) {
                return "今天 " + sentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else if (sentTime.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
                return "昨天 " + sentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                return sentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
        } catch (Exception e) {
            return sendTime; // 如果解析失败，返回原始时间
        }
    }

    /**
     *消息附件VO
     */
    @Data
    @Schema(description = "消息附件")
    public static class MessageAttachmentVO {
        @Schema(description = "附件ID")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private String id;

        @Schema(description = "附件名称")
        private String name;

        @Schema(description = "附件大小")
        private Long size;

        @Schema(description = "附件类型")
        private String type;

        @Schema(description = "上传时间")
        private String uploadTime;
    }
}
