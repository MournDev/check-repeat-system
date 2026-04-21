package com.abin.checkrepeatsystem.teacher.vo;

import lombok.Data;

import java.util.List;

/**
 * 相似度阈值设置VO
 */
@Data
public class SimilarityThresholdVO {
    /**
     * 全局相似度阈值
     */
    private Integer globalThreshold;

    /**
     * 学科/专业阈值列表
     */
    private List<CategorySimilarityThresholdVO> categoryThresholds;
}
