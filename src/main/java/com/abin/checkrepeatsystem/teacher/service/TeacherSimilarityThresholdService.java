package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.SimilarityThresholdDTO;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityThresholdVO;

/**
 * 相似度阈值设置服务接口
 */
public interface TeacherSimilarityThresholdService {

    /**
     * 获取相似度阈值设置
     *
     * @return 相似度阈值设置
     */
    Result<SimilarityThresholdVO> getThresholds();

    /**
     * 更新相似度阈值设置
     *
     * @param thresholdDTO 相似度阈值设置DTO
     * @return 更新结果
     */
    Result<Void> updateThresholds(SimilarityThresholdDTO thresholdDTO);
}
