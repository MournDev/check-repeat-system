package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.ReviewWorkflow;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.teacher.dto.ReviewWorkflowDTO;
import com.abin.checkrepeatsystem.teacher.dto.WorkflowStepDTO;
import com.abin.checkrepeatsystem.teacher.mapper.ReviewWorkflowMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewWorkflowService;
import com.abin.checkrepeatsystem.teacher.vo.ReviewWorkflowVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import com.abin.checkrepeatsystem.teacher.vo.WorkflowStepVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审核工作流配置服务实现
 */
@Service
public class TeacherReviewWorkflowServiceImpl implements TeacherReviewWorkflowService {

    @Autowired
    private ReviewWorkflowMapper reviewWorkflowMapper;

    @Autowired
    private BaseMapper<SysUser> sysUserMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取审核工作流配置
     *
     * @return 审核工作流配置
     */
    @Override
    public Result<ReviewWorkflowVO> getWorkflow() {
        try {
            // 查询工作流配置
            ReviewWorkflow workflow = reviewWorkflowMapper.selectById(1L);
            ReviewWorkflowVO vo = new ReviewWorkflowVO();

            if (workflow != null) {
                vo.setId(workflow.getId());
                vo.setName(workflow.getName());
                vo.setDescription(workflow.getDescription());
                vo.setEnabled(workflow.getEnabled());

                // 解析步骤
                if (workflow.getSteps() != null) {
                    try {
                        List<WorkflowStepVO> steps = new ArrayList<>();
                        List<WorkflowStepDTO> stepDTOs = objectMapper.readValue(workflow.getSteps(), objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowStepDTO.class));
                        for (WorkflowStepDTO stepDTO : stepDTOs) {
                            WorkflowStepVO stepVO = new WorkflowStepVO();
                            BeanUtils.copyProperties(stepDTO, stepVO);
                            steps.add(stepVO);
                        }
                        vo.setSteps(steps);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        vo.setSteps(new ArrayList<>());
                    }
                } else {
                    vo.setSteps(new ArrayList<>());
                }
            } else {
                // 默认工作流配置
                vo.setId(1L);
                vo.setName("默认审核工作流");
                vo.setDescription("默认的论文审核工作流");
                vo.setEnabled(true);
                vo.setSteps(new ArrayList<>());
            }

            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取工作流配置失败");
        }
    }

    /**
     * 更新审核工作流配置
     *
     * @param workflowDTO 审核工作流DTO
     * @return 更新结果
     */
    @Override
    public Result<Void> updateWorkflow(ReviewWorkflowDTO workflowDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            LocalDateTime now = LocalDateTime.now();

            // 解析步骤为JSON
            String stepsJson = null;
            if (workflowDTO.getSteps() != null) {
                stepsJson = objectMapper.writeValueAsString(workflowDTO.getSteps());
            }

            // 更新工作流配置
            ReviewWorkflow workflow = reviewWorkflowMapper.selectById(1L);
            if (workflow == null) {
                workflow = new ReviewWorkflow();
                workflow.setId(1L);
                workflow.setCreateTime(now);
                workflow.setCreateBy(userId);
            }
            workflow.setName(workflowDTO.getName());
            workflow.setDescription(workflowDTO.getDescription());
            workflow.setSteps(stepsJson);
            workflow.setEnabled(workflowDTO.getEnabled());
            workflow.setUpdateTime(now);
            workflow.setUpdateBy(userId);

            if (workflow.getId() == 1L && reviewWorkflowMapper.selectById(1L) != null) {
                reviewWorkflowMapper.updateById(workflow);
            } else {
                reviewWorkflowMapper.insert(workflow);
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"更新工作流配置失败");
        }
    }

    /**
     * 获取教师列表
     *
     * @return 教师列表
     */
    @Override
    public Result<List<StudentVO>> getTeachers() {
        try {
            // 查询所有教师
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getUserType, "teacher");
            List<SysUser> teachers = sysUserMapper.selectList(queryWrapper);

            // 转换为VO
            List<StudentVO> teacherVOs = new ArrayList<>();
            for (SysUser teacher : teachers) {
                StudentVO vo = new StudentVO();
                vo.setId(teacher.getId());
                vo.setName(teacher.getRealName());
                teacherVOs.add(vo);
            }

            return Result.success(teacherVOs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取教师列表失败");
        }
    }
}
