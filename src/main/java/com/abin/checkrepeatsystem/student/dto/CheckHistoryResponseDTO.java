package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 查重历史响应DTO
 */
@Data
@Schema(description = "查重历史响应DTO")
public class CheckHistoryResponseDTO {
    
    @Schema(description = "历史记录列表")
    private List<CheckHistoryDTO> history;
    
    @Schema(description = "趋势分析")
    private TrendAnalysisDTO trendAnalysis;
    
    @Schema(description = "论文信息")
    private PaperInfoDTO paperInfo;

    @Schema(description = "统计数据")
    private StatisticsDTO statistics;

    /**
     * 统计数据DTO
     */
    @Data
    public static class StatisticsDTO {
        @Schema(description = "改进率（百分比）")
        private Integer improvementRate;

        @Schema(description = "平均相似度")
        private BigDecimal averageSimilarity;

        @Schema(description = "改进速度描述")
        private String improvementSpeed;
    }

    /**
     * 趋势分析DTO
     */
    @Data
    public static class TrendAnalysisDTO {
        @Schema(description = "趋势方向")
        private String direction;
        
        @Schema(description = "总改进值")
        private BigDecimal totalImprovement;
        
        @Schema(description = "平均每次改进值")
        private BigDecimal averageImprovementPerVersion;
        
        @Schema(description = "最佳版本")
        private Integer bestVersion;
    }
    
    /**
     * 论文信息DTO
     */
    @Data
    public static class PaperInfoDTO {
        @Schema(description = "论文标题")
        private String title;
        
        @Schema(description = "当前相似度")
        private BigDecimal currentSimilarity;
        
        @Schema(description = "最低相似度")
        private BigDecimal lowestSimilarity;
        
        @Schema(description = "版本数量")
        private Integer versionCount;
    }
}