package com.abin.checkrepeatsystem.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 *文件上传响应VO
 */
@Data
@Schema(description = "文件上传响应")
public class FileUploadVO {

    @Schema(description = "文件 ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull(message = "文件 ID 不能为空")
    private Long id;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件URL")
    private String url;

    @Schema(description = "上传时间")
    private String uploadTime;
}