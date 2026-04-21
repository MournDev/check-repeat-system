package com.abin.checkrepeatsystem.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 *消息发送请求DTO
 */
@Data
@ApiModel("消息发送请求")
public class MessageSendDTO {

    @ApiModelProperty("会话ID")
    @NotNull(message = "会话ID不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sessionId;

    @ApiModelProperty("接收者ID")
    @NotNull(message = "接收者ID不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long receiverId;

    @ApiModelProperty("消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @ApiModelProperty("附件ID列表")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private List<String> attachmentIds;

    @ApiModelProperty("消息类型")
    private String messageType = "TEXT";

    @ApiModelProperty("关联类型")
    private String relatedType;

    @ApiModelProperty("关联ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long relatedId;
}