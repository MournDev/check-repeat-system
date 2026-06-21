package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 *聊天记录导出请求DTO
 */
@Data
@Schema(description = "聊天记录导出请求")
public class ChatExportDTO {

    @Schema(description = "会话ID")
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @Schema(description = "导出格式")
    @NotBlank(message = "导出格式不能为空")
    private String format = "pdf";

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}