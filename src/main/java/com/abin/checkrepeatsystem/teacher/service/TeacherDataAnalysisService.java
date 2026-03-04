package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.user.vo.PageResultVO;
import com.abin.checkrepeatsystem.teacher.dto.TrendQueryDTO;
import com.abin.checkrepeatsystem.teacher.dto.TrendResultDTO;
import com.abin.checkrepeatsystem.teacher.vo.CollegeDistributionVO;
import com.abin.checkrepeatsystem.teacher.vo.ReviewStatusVO;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityDistributionVO;
import com.abin.checkrepeatsystem.teacher.vo.TrendDataVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

public interface TeacherDataAnalysisService {

    /**
     * 获取审核趋势数据
     */
    TrendResultDTO getReviewTrend(TrendQueryDTO query);

    /**
     * 获取审核统计数据
     */
    Map<String, Object> getReviewStats(Long teacherId, String timeRange);

    /**
     * 获取详细数据表格
     */
    PageResultVO<TrendDataVO> getDetailData(TrendQueryDTO query, Integer page, Integer size);

    /**
     * 导出数据
     */
    void exportTrendData(TrendQueryDTO query, HttpServletResponse response);

    /**
     * 获取审核状态分布数据
     */
    List<ReviewStatusVO> getReviewStatusDistribution(Long teacherId, String timeRange);

    /**
     * 获取相似度分布数据
     */
    List<SimilarityDistributionVO> getSimilarityDistribution(Long teacherId, String timeRange);

    /**
     * 获取学院分布数据
     */
    List<CollegeDistributionVO> getCollegeDistribution(Long teacherId, String timeRange);
}
