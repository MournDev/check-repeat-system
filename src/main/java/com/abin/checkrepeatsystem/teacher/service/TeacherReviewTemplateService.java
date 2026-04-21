package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.ReviewTemplateDTO;
import com.abin.checkrepeatsystem.teacher.vo.ReviewTemplateVO;

import java.util.List;

/**
 * 审核意见模板服务接口
 */
public interface TeacherReviewTemplateService {

    /**
     * 获取模板列表
     *
     * @return 模板列表
     */
    Result<List<ReviewTemplateVO>> getTemplates();

    /**
     * 创建模板
     *
     * @param templateDTO 模板DTO
     * @return 创建的模板
     */
    Result<ReviewTemplateVO> createTemplate(ReviewTemplateDTO templateDTO);

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param templateDTO 模板DTO
     * @return 更新后的模板
     */
    Result<ReviewTemplateVO> updateTemplate(Long templateId, ReviewTemplateDTO templateDTO);

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 删除结果
     */
    Result<Void> deleteTemplate(Long templateId);

    /**
     * 使用模板
     *
     * @param templateId 模板ID
     * @return 使用结果
     */
    Result<Void> useTemplate(Long templateId);
}
