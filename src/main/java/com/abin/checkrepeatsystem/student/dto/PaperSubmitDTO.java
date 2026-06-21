package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论文提交版本DTO（用于前端版本列表展示）
 */
@Data
@Schema(description = "论文提交版本DTO")
public class PaperSubmitDTO {

    @Schema(description = "提交记录ID")
    private Long id;

    @Schema(description = "提交版本号")
    private Integer submitVersion;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "备注")
    private String remark;
}