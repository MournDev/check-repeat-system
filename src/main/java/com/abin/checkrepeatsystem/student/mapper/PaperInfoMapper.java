package com.abin.checkrepeatsystem.student.mapper;

import com.abin.checkrepeatsystem.admin.vo.CollegePaperStatsVO;
import com.abin.checkrepeatsystem.admin.vo.MajorPaperStatsVO;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.teacher.vo.CollegeDistributionVO;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityDistributionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaperInfoMapper extends BaseMapper<PaperInfo> {

    PaperInfo selectLatestPaper(Long studentId);

    /**
     * 获取相似度分布数据
     */
    List<SimilarityDistributionVO> selectSimilarityDistribution(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 获取学院分布数据
     */
    List<CollegeDistributionVO> selectCollegeDistribution(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 统计指定教师在时间范围内的论文总数
     */
    Integer countTotalPapers(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Integer countTeacherPapers(Long teacherId);
    
    /**
     * 获取学院论文分布统计（管理员用）
     */
    List<CollegePaperStatsVO> selectCollegePaperStats();
    
    /**
     * 获取专业论文分布统计（管理员用）
     */
    List<MajorPaperStatsVO> selectMajorPaperStats();
}
