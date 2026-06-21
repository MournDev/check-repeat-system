package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 学术资源DTO
 */
@Data
@Schema(description = "学术资源DTO")
public class AcademicResourceDTO {
    
    @Schema(description = "资源ID")
    private Long resourceId;
    
    @Schema(description = "资源标题")
    private String title;
    
    @Schema(description = "资源类型")
    private String type;
    
    @Schema(description = "资源描述")
    private String description;
    
    @Schema(description = "资源URL")
    private String url;
    
    @Schema(description = "资源分类")
    private String category;
}