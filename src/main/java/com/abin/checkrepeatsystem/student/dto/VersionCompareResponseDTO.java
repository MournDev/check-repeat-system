package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 版本对比响应DTO
 */
@Data
@Schema(description = "版本对比响应DTO")
public class VersionCompareResponseDTO {
    
    @Schema(description = "起始版本")
    private Integer fromVersion;
    
    @Schema(description = "目标版本")
    private Integer toVersion;
    
    @Schema(description = "总体变化")
    private BigDecimal overallChange;
    
    @Schema(description = "章节对比列表")
    private List<SectionComparisonDTO> sectionComparison;
    
    /**
     * 章节对比DTO
     */
    @Data
    public static class SectionComparisonDTO {
        @Schema(description = "章节名称")
        private String name;
        
        @Schema(description = "起始版本相似度")
        private BigDecimal from;
        
        @Schema(description = "目标版本相似度")
        private BigDecimal to;
        
        @Schema(description = "变化值")
        private BigDecimal change;
    }
}