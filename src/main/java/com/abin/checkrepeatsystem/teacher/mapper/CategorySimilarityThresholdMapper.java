package com.abin.checkrepeatsystem.teacher.mapper;

import com.abin.checkrepeatsystem.pojo.entity.CategorySimilarityThreshold;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学科/专业相似度阈值Mapper
 */
@Mapper
public interface CategorySimilarityThresholdMapper extends BaseMapper<CategorySimilarityThreshold> {
}
