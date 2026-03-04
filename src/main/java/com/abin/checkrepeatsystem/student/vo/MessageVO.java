package com.abin.checkrepeatsystem.student.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 *消息VO
 */
@Data
@ApiModel("消息")
public class MessageVO {

    @ApiModelProperty("消息ID")
    private Long id;

    @ApiModelProperty("发送者ID")
    private Long senderId;

    @ApiModelProperty("发送者姓名")
    private String senderName;

    @ApiModelProperty("发送者角色")
    private String senderRole;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("发送时间")
    private String sendTime;

    @ApiModelProperty("消息状态")
    private String status;

    @ApiModelProperty("消息类型")
    private String messageType;

    @ApiModelProperty("附件列表")
    private List<MessageAttachmentVO> attachments;

    @ApiModelProperty("发送者头像")
    private String senderAvatar;

    /**
     *消息附件VO
     */
    @Data
    @ApiModel("消息附件")
    public static class MessageAttachmentVO {
        @ApiModelProperty("附件ID")
        private String id;

        @ApiModelProperty("附件名称")
        private String name;

        @ApiModelProperty("附件大小")
        private Long size;

        @ApiModelProperty("附件类型")
        private String type;

        @ApiModelProperty("上传时间")
        private String uploadTime;
    }
}