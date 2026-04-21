package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

/**
 * 学科/专业相似度阈值设置DTO
 */
@Data
public class CategorySimilarityThresholdDTO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 分类类型：college-学院，major-专业
     */
    private String categoryType;

    /**
     * 学院ID
     */
    private Long collegeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 相似度阈值
     */
    private Integer threshold;

    /**
     * 分类名称
     */
    private String categoryName;
}
