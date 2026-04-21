package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.ReviewTemplateDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewTemplateService;
import com.abin.checkrepeatsystem.teacher.vo.ReviewTemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核意见模板控制器
 */
@RestController
@RequestMapping("/api/teacher/review-templates")
public class TeacherReviewTemplateController {

    @Autowired
    private TeacherReviewTemplateService reviewTemplateService;

    /**
     * 获取模板列表
     *
     * @return 模板列表
     */
    @GetMapping
    public Result<List<ReviewTemplateVO>> getTemplates() {
        return reviewTemplateService.getTemplates();
    }

    /**
     * 创建模板
     *
     * @param templateDTO 模板DTO
     * @return 创建的模板
     */
    @PostMapping
    public Result<ReviewTemplateVO> createTemplate(@RequestBody ReviewTemplateDTO templateDTO) {
        return reviewTemplateService.createTemplate(templateDTO);
    }

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param templateDTO 模板DTO
     * @return 更新后的模板
     */
    @PutMapping("/{templateId}")
    public Result<ReviewTemplateVO> updateTemplate(@PathVariable Long templateId, @RequestBody ReviewTemplateDTO templateDTO) {
        return reviewTemplateService.updateTemplate(templateId, templateDTO);
    }

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 删除结果
     */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable Long templateId) {
        return reviewTemplateService.deleteTemplate(templateId);
    }

    /**
     * 使用模板
     *
     * @param templateId 模板ID
     * @return 使用结果
     */
    @PostMapping("/{templateId}/use")
    public Result<Void> useTemplate(@PathVariable Long templateId) {
        return reviewTemplateService.useTemplate(templateId);
    }
}
