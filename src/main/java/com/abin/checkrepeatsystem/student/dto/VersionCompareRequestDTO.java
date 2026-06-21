package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 版本对比请求DTO
 */
@Data
@Schema(description = "版本对比请求DTO")
public class VersionCompareRequestDTO {
    
    @Schema(description = "起始版本")
    @NotNull(message = "起始版本不能为空")
    private Integer fromVersion;
    
    @Schema(description = "目标版本")
    @NotNull(message = "目标版本不能为空")
    private Integer toVersion;
}