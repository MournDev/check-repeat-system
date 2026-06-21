package com.abin.checkrepeatsystem.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 *消息发送请求DTO
 */
@Data
@Schema(description = "消息发送请求")
public class MessageSendDTO {

    @Schema(description = "会话ID（可选，如果为空则自动创建）")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sessionId;

    @Schema(description = "接收者ID")
    @NotNull(message = "接收者ID不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long receiverId;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "附件ID列表")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private List<String> attachmentIds;

    @Schema(description = "消息类型")
    private String messageType = "TEXT";

    @Schema(description = "关联类型")
    private String relatedType;

    @Schema(description = "关联ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long relatedId;
}