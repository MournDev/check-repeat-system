package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.SimilarityThresholdDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherSimilarityThresholdService;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityThresholdVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 相似度阈值设置控制器
 */
@RestController
@RequestMapping("/api/teacher/similarity-thresholds")
public class TeacherSimilarityThresholdController {

    @Autowired
    private TeacherSimilarityThresholdService similarityThresholdService;

    /**
     * 获取相似度阈值设置
     *
     * @return 相似度阈值设置
     */
    @GetMapping
    public Result<SimilarityThresholdVO> getThresholds() {
        return similarityThresholdService.getThresholds();
    }

    /**
     * 更新相似度阈值设置
     *
     * @param thresholdDTO 相似度阈值设置DTO
     * @return 更新结果
     */
    @PutMapping
    public Result<Void> updateThresholds(@RequestBody SimilarityThresholdDTO thresholdDTO) {
        return similarityThresholdService.updateThresholds(thresholdDTO);
    }
}
