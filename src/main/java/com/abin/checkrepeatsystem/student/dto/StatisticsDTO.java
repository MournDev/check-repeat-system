package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统计分析数据DTO
 */
@Data
@Schema(description = "统计分析数据DTO")
public class StatisticsDTO {
    
    @Schema(description = "改进率")
    private Integer improvementRate;
    
    @Schema(description = "平均相似度")
    private BigDecimal averageSimilarity;
    
    @Schema(description = "改进速度")
    private String improvementSpeed;
    
    @Schema(description = "总查重次数")
    private Integer totalChecks;
    
    @Schema(description = "首次查重相似度")
    private BigDecimal firstCheckSimilarity;
    
    @Schema(description = "最新查重相似度")
    private BigDecimal latestCheckSimilarity;
}