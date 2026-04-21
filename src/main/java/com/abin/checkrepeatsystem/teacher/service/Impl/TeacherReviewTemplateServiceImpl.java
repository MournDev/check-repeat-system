package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.ReviewTemplate;
import com.abin.checkrepeatsystem.teacher.dto.ReviewTemplateDTO;
import com.abin.checkrepeatsystem.teacher.mapper.ReviewTemplateMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewTemplateService;
import com.abin.checkrepeatsystem.teacher.vo.ReviewTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审核意见模板服务实现
 */
@Service
public class TeacherReviewTemplateServiceImpl implements TeacherReviewTemplateService {

    @Autowired
    private ReviewTemplateMapper reviewTemplateMapper;

    /**
     * 获取模板列表
     *
     * @return 模板列表
     */
    @Override
    public Result<List<ReviewTemplateVO>> getTemplates() {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询模板列表
            LambdaQueryWrapper<ReviewTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReviewTemplate::getCreateBy, userId)
                    .or()
                    .eq(ReviewTemplate::getIsPublic, true);
            List<ReviewTemplate> templates = reviewTemplateMapper.selectList(queryWrapper);
            
            // 转换为VO
            List<ReviewTemplateVO> templateVOs = new ArrayList<>();
            for (ReviewTemplate template : templates) {
                ReviewTemplateVO vo = new ReviewTemplateVO();
                BeanUtils.copyProperties(template, vo);
                templateVOs.add(vo);
            }
            
            return Result.success(templateVOs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取模板列表失败");
        }
    }

    /**
     * 创建模板
     *
     * @param templateDTO 模板DTO
     * @return 创建的模板
     */
    @Override
    public Result<ReviewTemplateVO> createTemplate(ReviewTemplateDTO templateDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 创建模板实体
            ReviewTemplate template = new ReviewTemplate();
            BeanUtils.copyProperties(templateDTO, template);
            template.setUsageCount(0);
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            template.setCreateBy(userId);
            template.setUpdateBy(userId);
            
            // 保存模板
            reviewTemplateMapper.insert(template);
            
            // 转换为VO
            ReviewTemplateVO vo = new ReviewTemplateVO();
            BeanUtils.copyProperties(template, vo);
            
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"创建模板失败");
        }
    }

    /**
     * 更新模板
     *
     * @param templateId 模板ID
     * @param templateDTO 模板DTO
     * @return 更新后的模板
     */
    @Override
    public Result<ReviewTemplateVO> updateTemplate(Long templateId, ReviewTemplateDTO templateDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询模板
            ReviewTemplate template = reviewTemplateMapper.selectById(templateId);
            if (template == null) {
                return Result.error(ResultCode.SYSTEM_ERROR,"模板不存在");
            }
            
            // 检查权限
            if (!template.getCreateBy().equals(userId) && !template.getIsPublic()) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限修改此模板");
            }
            
            // 更新模板
            BeanUtils.copyProperties(templateDTO, template);
            template.setUpdateTime(LocalDateTime.now());
            template.setUpdateBy(userId);
            
            // 保存模板
            reviewTemplateMapper.updateById(template);
            
            // 转换为VO
            ReviewTemplateVO vo = new ReviewTemplateVO();
            BeanUtils.copyProperties(template, vo);
            
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"更新模板失败");
        }
    }

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 删除结果
     */
    @Override
    public Result<Void> deleteTemplate(Long templateId) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询模板
            ReviewTemplate template = reviewTemplateMapper.selectById(templateId);
            if (template == null) {
                return Result.error(ResultCode.SYSTEM_ERROR,"模板不存在");
            }
            
            // 检查权限
            if (!template.getCreateBy().equals(userId)) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限删除此模板");
            }
            
            // 删除模板
            reviewTemplateMapper.deleteById(templateId);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"删除模板失败");
        }
    }

    /**
     * 使用模板
     *
     * @param templateId 模板ID
     * @return 使用结果
     */
    @Override
    public Result<Void> useTemplate(Long templateId) {
        try {
            // 查询模板
            ReviewTemplate template = reviewTemplateMapper.selectById(templateId);
            if (template == null) {
                return Result.error(ResultCode.SYSTEM_ERROR,"模板不存在");
            }
            
            // 增加使用次数
            LambdaUpdateWrapper<ReviewTemplate> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ReviewTemplate::getId, templateId)
                    .set(ReviewTemplate::getUsageCount, template.getUsageCount() + 1)
                    .set(ReviewTemplate::getLastUsed, LocalDateTime.now());
            reviewTemplateMapper.update(updateWrapper);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"使用模板失败");
        }
    }
}
