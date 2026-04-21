package com.abin.checkrepeatsystem.teacher.mapper;

import com.abin.checkrepeatsystem.pojo.entity.SimilarityThreshold;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 相似度阈值Mapper
 */
@Mapper
public interface SimilarityThresholdMapper extends BaseMapper<SimilarityThreshold> {
}
