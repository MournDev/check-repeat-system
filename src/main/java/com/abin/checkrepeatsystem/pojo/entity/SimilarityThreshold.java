package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 相似度阈值设置实体类
 */
@Data
@TableName("similarity_threshold")
public class SimilarityThreshold extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 全局相似度阈值
     */
    private Integer globalThreshold;
}
