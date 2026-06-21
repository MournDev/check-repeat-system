package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 检查清单项DTO
 */
@Data
@Schema(description = "检查清单项DTO")
public class ChecklistItemDTO {
    
    @Schema(description = "检查项ID")
    private Long itemId;
    
    @Schema(description = "检查项文本")
    private String text;
    
    @Schema(description = "是否已检查")
    private Boolean checked;
}