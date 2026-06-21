package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 相似度趋势数据DTO
 */
@Data
@Schema(description = "相似度趋势数据DTO")
public class SimilarityTrendDTO {
    
    @Schema(description = "日期列表")
    private List<String> dates;
    
    @Schema(description = "版本标签列表")
    private List<String> versions;
    
    @Schema(description = "相似度列表")
    private List<BigDecimal> similarities;
    
    // 兼容旧版本的setter方法
    public void setVersions(List<String> versions) {
        this.versions = versions;
    }
}