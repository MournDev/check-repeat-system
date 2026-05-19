package com.abin.checkrepeatsystem.teacher.mapper;

import com.abin.checkrepeatsystem.pojo.entity.ReviewDraft;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审核草稿Mapper
 */
@Mapper
public interface ReviewDraftMapper extends BaseMapper<ReviewDraft> {

    /**
     * 根据教师ID和论文ID查询草稿
     */
    ReviewDraft selectByTeacherAndPaper(@Param("teacherId") Long teacherId, @Param("paperId") Long paperId);

    /**
     * 根据教师ID查询所有草稿
     */
    List<ReviewDraft> selectByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 删除指定论文的草稿
     */
    int deleteByPaperId(@Param("paperId") Long paperId);
}