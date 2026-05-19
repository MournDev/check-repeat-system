package com.abin.checkrepeatsystem.teacher.mapper;

import com.abin.checkrepeatsystem.pojo.entity.ReviewTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审核意见模板Mapper
 */
@Mapper
public interface ReviewTemplateMapper extends BaseMapper<ReviewTemplate> {

    List<ReviewTemplate> selectTemplatesWithScenarios(@Param("userId") Long userId);

    ReviewTemplate selectByIdWithScenarios(@Param("id") Long id);
}
