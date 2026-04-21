package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

import java.util.List;

/**
 * 相似度阈值设置DTO
 */
@Data
public class SimilarityThresholdDTO {
    /**
     * 全局相似度阈值
     */
    private Integer globalThreshold;

    /**
     * 学科/专业阈值列表
     */
    private List<CategorySimilarityThresholdDTO> categoryThresholds;
}
