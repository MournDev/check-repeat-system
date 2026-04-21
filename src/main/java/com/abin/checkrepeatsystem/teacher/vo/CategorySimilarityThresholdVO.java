package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 学科/专业相似度阈值设置VO
 */
@Data
public class CategorySimilarityThresholdVO {
    /**
     * 主键ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 分类类型：college-学院，major-专业
     */
    private String categoryType;

    /**
     * 学院ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long collegeId;

    /**
     * 专业ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
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
