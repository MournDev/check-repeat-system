package com.abin.checkrepeatsystem.student.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *消息会话VO
 */
@Data
@Schema(description = "消息会话")
public class MessageSessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话名称")
    private String name;

    @Schema(description = "会话类型")
    private String type;

    @Schema(description = "参与用户列表")
    private List<SessionMemberVO> members;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "最后消息时间")
    private String lastTime;

    @Schema(description = "未读消息数")
    private Integer unreadCount;

    @Schema(description = "会话头像")
    private String avatar;

    /**
     *会话成员VO
     */
    @Data
    @Schema(description = "会话成员")
    public static class SessionMemberVO {
        @Schema(description = "用户ID")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long userId;

        @Schema(description = "用户姓名")
        private String userName;

        @Schema(description = "用户角色")
        private String userRole;

        @Schema(description = "用户头像")
        private String avatar;
    }
}