package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 查重历史记录DTO
 */
@Data
@Schema(description = "查重历史记录DTO")
public class CheckHistoryDTO {
    
    @Schema(description = "版本号（展示序号）")
    private Integer version;

    @Schema(description = "实际提交版本号（对应paper_submit.submit_version）")
    private Integer submitVersion;
    
    @Schema(description = "报告ID")
    private String reportId;
    
    @Schema(description = "查重时间")
    private LocalDateTime checkTime;
    
    @Schema(description = "相似度")
    private BigDecimal similarity;
    
    @Schema(description = "评级")
    private String rating;
    
    @Schema(description = "是否为当前版本")
    private Boolean isCurrent;
    
    @Schema(description = "修改说明")
    private String changes;
    
    @Schema(description = "相比上一版本的改进")
    private BigDecimal improvementFromPrevious;
    
    @Schema(description = "各章节变化情况")
    private Map<String, SectionChangeDTO> sectionChanges;
    
    /**
     * 章节变化DTO
     */
    @Data
    public static class SectionChangeDTO {
        @Schema(description = "前一个版本的相似度")
        private BigDecimal from;
        
        @Schema(description = "当前版本的相似度")
        private BigDecimal to;
        
        @Schema(description = "变化值")
        private BigDecimal change;
    }
}