package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 学科/专业相似度阈值设置实体类
 */
@Data
@TableName("category_similarity_threshold")
public class CategorySimilarityThreshold extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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
}
